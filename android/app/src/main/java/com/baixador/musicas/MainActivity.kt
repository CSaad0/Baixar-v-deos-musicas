package com.baixador.musicas

import android.Manifest
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var link: TextInputEditText
    private lateinit var formato: Spinner
    private lateinit var qualidade: Spinner
    private lateinit var btnBaixar: MaterialButton
    private lateinit var progresso: LinearProgressIndicator
    private lateinit var status: TextView

    private var pronto = false
    private var baixando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        link = findViewById(R.id.link)
        formato = findViewById(R.id.formato)
        qualidade = findViewById(R.id.qualidade)
        btnBaixar = findViewById(R.id.btnBaixar)
        progresso = findViewById(R.id.progresso)
        status = findViewById(R.id.status)

        formato.adapter = adaptador(Formatos.TODOS.map { it.nome })
        formato.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                qualidade.adapter = adaptador(Formatos.TODOS[pos].qualidades)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<MaterialButton>(R.id.btnColar).setOnClickListener { colar() }
        btnBaixar.setOnClickListener { iniciar() }
        btnBaixar.isEnabled = false

        receberCompartilhamento(intent)
        inicializar()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receberCompartilhamento(intent)
    }

    private fun adaptador(itens: List<String>) =
        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, itens)

    private fun receberCompartilhamento(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val texto = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val url = Regex("https?://\\S+").find(texto)?.value ?: texto
        link.setText(url)
    }

    private fun colar() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val texto = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (texto.isNullOrEmpty()) {
            Toast.makeText(this, "Área de transferência vazia", Toast.LENGTH_SHORT).show()
        } else {
            link.setText(texto)
        }
    }

    /** Prepara o yt-dlp/FFmpeg embutidos e tenta atualizar o yt-dlp. */
    private fun inicializar() {
        progresso.isIndeterminate = true
        Thread {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                runOnUiThread { status.text = "Verificando atualizações..." }
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.STABLE)
                } catch (_: Exception) {
                    // Sem internet ou limite da API: segue com a versão embutida
                }
                runOnUiThread {
                    pronto = true
                    btnBaixar.isEnabled = true
                    progresso.isIndeterminate = false
                    progresso.progress = 0
                    status.text = "Pronto."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progresso.isIndeterminate = false
                    status.text = "Falha ao iniciar: ${e.message}"
                }
            }
        }.start()
    }

    private fun iniciar() {
        if (!pronto || baixando) return
        val url = link.text?.toString()?.trim().orEmpty()
        if (url.isEmpty()) {
            Toast.makeText(this, "Cole o link primeiro", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }

        val fmt = Formatos.TODOS[formato.selectedItemPosition]
        val qual = qualidade.selectedItem?.toString() ?: fmt.qualidades.first()

        baixando = true
        btnBaixar.isEnabled = false
        progresso.isIndeterminate = true
        status.text = "Iniciando..."

        Thread {
            val tmp = File(cacheDir, "dl_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                val req = YoutubeDLRequest(url)
                Formatos.opcoes(fmt, qual, tmp).forEach { op ->
                    if (op.size == 1) req.addOption(op[0]) else req.addOption(op[0], op[1])
                }
                YoutubeDL.getInstance().execute(req, "baixar", false) { pct, _, _ ->
                    runOnUiThread {
                        progresso.isIndeterminate = pct <= 0f || pct >= 100f
                        progresso.progress = pct.toInt()
                        status.text = if (pct >= 100f) "Convertendo..." else "Baixando... %.1f%%".format(pct)
                    }
                }
                val salvos = salvarEmDownloads(tmp)
                runOnUiThread {
                    terminar("Concluído!")
                    AlertDialog.Builder(this)
                        .setTitle("Download concluído")
                        .setMessage(salvos.joinToString("\n") + "\n\nSalvo em Download/Baixador")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                val msg = (e.message ?: e.toString()).lines()
                    .filter { it.isNotBlank() }.takeLast(4).joinToString("\n")
                runOnUiThread {
                    terminar("Erro no download.")
                    AlertDialog.Builder(this)
                        .setTitle("Erro")
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } finally {
                tmp.deleteRecursively()
            }
        }.start()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) iniciar()
        else Toast.makeText(this, "Permissão necessária para salvar o arquivo", Toast.LENGTH_LONG).show()
    }

    private fun terminar(msg: String) {
        baixando = false
        btnBaixar.isEnabled = true
        progresso.isIndeterminate = false
        progresso.progress = if (msg.startsWith("Concl")) 100 else 0
        status.text = msg
    }

    /** Copia os arquivos baixados para a pasta pública Download/Baixador. */
    private fun salvarEmDownloads(pasta: File): List<String> {
        val arquivos = pasta.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in Formatos.MIME }
            .orEmpty()
        if (arquivos.isEmpty()) throw IllegalStateException("Nenhum arquivo foi gerado.")

        for (arq in arquivos) {
            val mime = Formatos.MIME.getValue(arq.extension.lowercase())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valores = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, arq.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Baixador")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                    ?: throw IllegalStateException("Não foi possível criar o arquivo.")
                contentResolver.openOutputStream(uri)!!.use { out -> arq.inputStream().use { it.copyTo(out) } }
                valores.clear()
                valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, valores, null, null)
            } else {
                @Suppress("DEPRECATION")
                val destino = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Baixador"
                ).apply { mkdirs() }
                val final = File(destino, arq.name)
                arq.copyTo(final, overwrite = true)
                MediaScannerConnection.scanFile(this, arrayOf(final.absolutePath), arrayOf(mime), null)
            }
        }
        return arquivos.map { it.name }
    }
}

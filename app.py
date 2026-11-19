"""Baixador de Músicas e Vídeos - interface gráfica (Tkinter)."""

from __future__ import annotations

import os
import queue
import threading
import tkinter as tk
from tkinter import filedialog, messagebox, ttk

from downloader import FORMATOS, baixar, qualidades_para

PASTA_PADRAO = os.path.join(os.path.expanduser("~"), "Downloads")


class App(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("Baixador de Músicas")
        self.geometry("560x330")
        self.minsize(480, 330)
        self.eventos: queue.Queue = queue.Queue()
        self.baixando = False

        frm = ttk.Frame(self, padding=16)
        frm.pack(fill="both", expand=True)
        frm.columnconfigure(1, weight=1)

        # Link
        ttk.Label(frm, text="Link:").grid(row=0, column=0, sticky="w", pady=6)
        self.var_url = tk.StringVar()
        self.ent_url = ttk.Entry(frm, textvariable=self.var_url)
        self.ent_url.grid(row=0, column=1, sticky="ew", pady=6)
        ttk.Button(frm, text="Colar", command=self.colar).grid(row=0, column=2, padx=(6, 0))

        # Tipo de arquivo
        ttk.Label(frm, text="Tipo de arquivo:").grid(row=1, column=0, sticky="w", pady=6)
        self.var_formato = tk.StringVar(value="MP3")
        cb_fmt = ttk.Combobox(frm, textvariable=self.var_formato, values=list(FORMATOS),
                              state="readonly", width=12)
        cb_fmt.grid(row=1, column=1, sticky="w", pady=6)
        cb_fmt.bind("<<ComboboxSelected>>", lambda _e: self.atualizar_qualidades())

        # Qualidade
        ttk.Label(frm, text="Qualidade:").grid(row=2, column=0, sticky="w", pady=6)
        self.var_qualidade = tk.StringVar()
        self.cb_qual = ttk.Combobox(frm, textvariable=self.var_qualidade, state="readonly", width=12)
        self.cb_qual.grid(row=2, column=1, sticky="w", pady=6)
        self.atualizar_qualidades()

        # Pasta de destino
        ttk.Label(frm, text="Salvar em:").grid(row=3, column=0, sticky="w", pady=6)
        self.var_pasta = tk.StringVar(value=PASTA_PADRAO)
        ttk.Entry(frm, textvariable=self.var_pasta).grid(row=3, column=1, sticky="ew", pady=6)
        ttk.Button(frm, text="Escolher...", command=self.escolher_pasta).grid(row=3, column=2, padx=(6, 0))

        # Botão e progresso
        self.btn = ttk.Button(frm, text="Baixar", command=self.iniciar)
        self.btn.grid(row=4, column=0, columnspan=3, pady=(14, 8), ipadx=20)
        self.barra = ttk.Progressbar(frm, maximum=100)
        self.barra.grid(row=5, column=0, columnspan=3, sticky="ew")
        self.var_status = tk.StringVar(value="Pronto.")
        ttk.Label(frm, textvariable=self.var_status).grid(row=6, column=0, columnspan=3, sticky="w", pady=6)

        self.ent_url.focus()
        self.bind("<Return>", lambda _e: self.iniciar())
        self.after(100, self.processar_eventos)

    def colar(self) -> None:
        try:
            self.var_url.set(self.clipboard_get().strip())
        except tk.TclError:
            pass

    def escolher_pasta(self) -> None:
        pasta = filedialog.askdirectory(initialdir=self.var_pasta.get() or PASTA_PADRAO)
        if pasta:
            self.var_pasta.set(pasta)

    def atualizar_qualidades(self) -> None:
        opcoes = qualidades_para(self.var_formato.get())
        self.cb_qual["values"] = opcoes
        if self.var_qualidade.get() not in opcoes:
            self.var_qualidade.set(opcoes[0])

    def iniciar(self) -> None:
        if self.baixando:
            return
        url = self.var_url.get().strip()
        if not url:
            messagebox.showwarning("Atenção", "Cole o link da música primeiro.")
            return
        self.baixando = True
        self.btn.config(state="disabled")
        self.barra["value"] = 0
        self.var_status.set("Iniciando...")
        args = (url, self.var_formato.get(), self.var_qualidade.get(), self.var_pasta.get())
        threading.Thread(target=self.trabalhar, args=args, daemon=True).start()

    def trabalhar(self, url: str, formato: str, qualidade: str, pasta: str) -> None:
        try:
            titulo = baixar(url, formato, qualidade, pasta,
                            progresso=lambda p, m: self.eventos.put(("progresso", p, m)))
            self.eventos.put(("ok", titulo))
        except Exception as e:  # noqa: BLE001 - mostra qualquer erro ao usuário
            self.eventos.put(("erro", str(e)))

    def processar_eventos(self) -> None:
        try:
            while True:
                ev = self.eventos.get_nowait()
                if ev[0] == "progresso":
                    self.barra["value"] = ev[1]
                    self.var_status.set(ev[2])
                elif ev[0] == "ok":
                    self.finalizar(f"Concluído: {ev[1]}")
                    messagebox.showinfo("Pronto", f"Download concluído!\n\n{ev[1]}")
                elif ev[0] == "erro":
                    self.finalizar("Erro no download.")
                    messagebox.showerror("Erro", ev[1])
        except queue.Empty:
            pass
        self.after(100, self.processar_eventos)

    def finalizar(self, msg: str) -> None:
        self.baixando = False
        self.btn.config(state="normal")
        self.var_status.set(msg)


if __name__ == "__main__":
    App().mainloop()

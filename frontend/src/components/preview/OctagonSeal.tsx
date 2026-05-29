/** Sello octogonal de advertencia según Ley 27.642 / Decreto 151/2022.
 *  El octágono sigue las proporciones normativas: fondo negro, texto blanco,
 *  borde blanco, tipografía en negrita.
 */
export function OctagonSeal({ label }: { label: string }) {
  // Línea corta del sello (quitar "EXCESO EN ")
  const shortLabel = label.replace(/^EXCESO EN /, '')

  return (
    <div
      className="relative flex items-center justify-center"
      style={{ width: 80, height: 80 }}
      title={label}
    >
      {/* Fondo octogonal */}
      <svg viewBox="0 0 100 100" className="absolute inset-0 w-full h-full">
        <polygon
          points="29,2 71,2 98,29 98,71 71,98 29,98 2,71 2,29"
          fill="#000"
          stroke="#fff"
          strokeWidth="4"
        />
      </svg>

      {/* Texto centrado */}
      <div className="relative z-10 text-center px-1">
        <p className="text-white font-black leading-none" style={{ fontSize: 9 }}>
          EXCESO EN
        </p>
        <p className="text-white font-black leading-tight mt-0.5" style={{ fontSize: 8 }}>
          {shortLabel}
        </p>
      </div>
    </div>
  )
}

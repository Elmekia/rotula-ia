interface SkeletonRowProps {
  cols?: number
}

export function SkeletonRow({ cols = 5 }: SkeletonRowProps) {
  return (
    <div className="grid gap-4 px-6 py-4 border-b border-slate-100 last:border-0"
         style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }}>
      {Array.from({ length: cols }).map((_, i) => (
        <div key={i} className="h-4 bg-slate-200 rounded animate-pulse" />
      ))}
    </div>
  )
}

export function SkeletonTable({ rows = 5, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <>
      {Array.from({ length: rows }).map((_, i) => (
        <SkeletonRow key={i} cols={cols} />
      ))}
    </>
  )
}

import {
  createElement,
  useEffect,
  useRef,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type ReactNode,
} from "react"
import { X } from "lucide-react"

type TextProps = HTMLAttributes<HTMLElement> & {
  as?: "span" | "p" | "h1" | "h2" | "h3" | "h4"
}

export function Text({ as = "span", children, ...props }: TextProps) {
  return createElement(as, props, children)
}

export function Button({
  className = "",
  variant = "default",
  type = "button",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "default" | "primary" | "text" | "icon" | "unstyled"
}) {
  return createElement("button", {
    ...props,
    type,
    className: `button button--${variant} ${className}`,
  })
}

export function Card({
  className = "",
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return <div className={`card ${className}`} {...props} />
}

export function Badge({
  children,
  tone = "blue",
  className = "",
}: {
  children: ReactNode
  tone?: "blue" | "coral" | "green" | "amber" | "purple" | "neutral"
  className?: string
}) {
  return <span className={`badge badge--${tone} ${className}`}>{children}</span>
}

export function Modal({
  title,
  children,
  onClose,
}: {
  title: string
  children: ReactNode
  onClose: () => void
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    dialog?.showModal()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"
    return () => {
      dialog?.close()
      document.body.style.overflow = previousOverflow
    }
  }, [])

  return (
    <dialog
      ref={ref}
      className="modal"
      aria-labelledby="modal-title"
      onCancel={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className="modal-header">
        <Text as="h2" id="modal-title">
          {title}
        </Text>
        <Button variant="icon" aria-label="Close dialog" onClick={onClose}>
          <X size={20} />
        </Button>
      </div>
      <div className="modal-content">{children}</div>
    </dialog>
  )
}

export function ProgressBar({
  value,
  tone = "blue",
  label = "Course progress",
}: {
  value: number
  tone?: string
  label?: string
}) {
  return (
    <div
      className={`progress progress--${tone}`}
      role="progressbar"
      aria-label={label}
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div className="progress-fill" style={{ width: `${value}%` }} />
    </div>
  )
}

export function ProgressRing({
  value,
  label,
}: {
  value: number
  label: string
}) {
  return (
    <div className="progress-ring" role="img" aria-label={`${value}% ${label}`}>
      <svg viewBox="0 0 100 100" aria-hidden="true">
        <circle className="ring-track" cx="50" cy="50" r="42" />
        <circle
          className="ring-fill"
          cx="50"
          cy="50"
          r="42"
          pathLength="100"
          strokeDasharray={`${value} 100`}
        />
      </svg>
      <div className="ring-content">
        <Text className="ring-value">
          {value}
          <Text className="ring-percent">%</Text>
        </Text>
        <Text className="ring-label">{label}</Text>
      </div>
    </div>
  )
}

import type { HTMLAttributes, ReactNode } from "react"

import { AlertCircle, CircleCheck, RotateCcw } from "lucide-react"

import { Button, Card, Text } from "./ui"

/* ==========================================================================
   1. SKELETON COMPONENTS (Khung chờ tải dữ liệu)
   ========================================================================== */

type SkeletonProps = HTMLAttributes<HTMLDivElement> & {
  width?: string | number

  height?: string | number

  radius?: string | number
}

/**
 * Khối Skeleton nguyên thủy có hiệu ứng nhấp nháy (pulse)
 */

export function Skeleton({
  width = "100%",

  height = 16,

  radius = 8,

  className = "",

  style,

  ...props
}: SkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse ${className}`}
      style={{
        width,

        height,

        borderRadius: radius,

        backgroundColor: "var(--line)",

        ...style,
      }}
      {...props}
    />
  )
}

/**
 * Skeleton cho Card chung (StudyOverview, DeadlineCard, Tracker...)
 */

export function CardSkeleton({
  height = 192,

  className = "",
}: {
  height?: number

  className?: string
}) {
  return (
    <Card
      className={`p-4.5 flex flex-col justify-between ${className}`}
      style={{ minHeight: height }}
      aria-busy="true"
      aria-label="Loading card"
    >
      <div className="flex items-center justify-between gap-2">
        <Skeleton width="38%" height={12} radius={6} />
        <Skeleton width={56} height={20} radius={6} />
      </div>
      <div className="flex flex-col gap-2.5 my-auto py-3">
        <Skeleton width="75%" height={22} radius={8} />
        <Skeleton width="50%" height={14} radius={6} />
      </div>
      <div className="flex items-center justify-between gap-3 pt-2">
        <Skeleton width="100%" height={8} radius={10} />
      </div>
    </Card>
  )
}

/**
 * Skeleton cho thẻ khóa học CourseCard (chuẩn chiều cao 230px, footer 44px)
 */

export function CourseCardSkeleton({ count = 1 }: { count?: number }) {
  return (
    <>
      {Array.from({ length: count }).map((_, index) => (
        <Card
          key={index}
          className="overflow-hidden flex flex-col justify-between"
          style={{ height: 230 }}
          aria-busy="true"
          aria-label="Loading course"
        >
          <div className="p-4 flex flex-col flex-1 justify-between bg-[#f1f5f9]">
            <div className="flex justify-between items-center">
              <Skeleton width={110} height={12} radius={4} />
              <Skeleton width={18} height={18} radius="50%" />
            </div>
            <div className="my-auto">
              <Skeleton width="80%" height={22} radius={6} className="mb-2" />
              <Skeleton width="45%" height={12} radius={4} />
            </div>
            <div className="flex items-center gap-3 pt-3">
              <Skeleton width="100%" height={4} radius={10} />
              <Skeleton width={32} height={12} radius={4} />
            </div>
          </div>
          <div
            className="flex items-center justify-between px-3.5"
            style={{ height: 44, borderTop: "1px solid var(--line)" }}
          >
            <Skeleton width={95} height={12} radius={4} />
            <Skeleton width={65} height={12} radius={4} />
          </div>
        </Card>
      ))}
    </>
  )
}

/**
 * Skeleton cho hàng bài tập AssignmentRow (min-height 108px hoặc 78px khi compact)
 */

export function AssignmentRowSkeleton({
  compact = false,

  count = 2,
}: {
  compact?: boolean

  count?: number
}) {
  return (
    <>
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className={`assignment-row ${
            compact ? "assignment-row--compact" : ""
          }`}
          aria-busy="true"
          aria-label="Loading assignment"
        >
          <Skeleton width={36} height={40} radius={10} className="shrink-0" />
          <div className="assignment-info flex flex-col justify-center gap-2">
            <Skeleton width="72%" height={15} radius={5} />
            <Skeleton width="40%" height={11} radius={4} />
            {!compact && (
              <div className="flex gap-1.5 mt-1">
                <Skeleton width={84} height={20} radius={6} />
                <Skeleton width={64} height={20} radius={6} />
              </div>
            )}
          </div>
        </div>
      ))}
    </>
  )
}

/* ==========================================================================
   2. EMPTY STATE (Trạng thái rỗng: Icon + Lời nhắn)
   ========================================================================== */

type EmptyStateProps = {
  icon?: ReactNode

  title?: string

  message: string

  actionLabel?: string

  onAction?: () => void

  className?: string
}

export function EmptyState({
  icon,

  title,

  message,

  actionLabel,

  onAction,

  className = "",
}: EmptyStateProps) {
  return (
    <div className={`classwork-empty ${className}`} role="status">
      {icon ?? <CircleCheck size={28} />}
      {title && <Text as="h3">{title}</Text>}
      <Text>{message}</Text>
      {actionLabel && onAction && (
        <Button variant="default" onClick={onAction} className="mt-1">
          {actionLabel}
        </Button>
      )}
    </div>
  )
}

/* ==========================================================================
   3. ERROR STATE (Trạng thái lỗi + Nút 'Thử lại')
   ========================================================================== */

type ErrorStateProps = {
  title?: string

  message?: string

  retryLabel?: string

  onRetry?: () => void

  className?: string
}

export function ErrorState({
  title = "Không thể tải dữ liệu",

  message = "Đã xảy ra sự cố kết nối với máy chủ. Vui lòng kiểm tra và thử lại.",

  retryLabel = "Thử lại",

  onRetry,

  className = "",
}: ErrorStateProps) {
  return (
    <div
      className={`classwork-empty ${className}`}
      role="alert"
      style={{
        borderColor: "color-mix(in srgb, var(--color-coral) 25%, var(--line))",

        background: "color-mix(in srgb, var(--coral-soft) 35%, var(--surface))",
      }}
    >
      <span
        className="subject-icon"
        style={{
          background: "var(--coral-soft)",

          color: "var(--color-coral)",
        }}
        aria-hidden="true"
      >
        <AlertCircle size={22} />
      </span>
      <Text as="h3">{title}</Text>
      <Text className="muted">{message}</Text>
      {onRetry && (
        <Button
          variant="default"
          onClick={onRetry}
          style={{
            background: "var(--coral-soft)",

            color: "var(--color-coral)",
          }}
        >
          <RotateCcw size={15} />
          {retryLabel}
        </Button>
      )}
    </div>
  )
}

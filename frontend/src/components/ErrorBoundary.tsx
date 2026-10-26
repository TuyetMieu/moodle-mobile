import { Component, type ReactNode } from "react"
import { RotateCw, TriangleAlert } from "lucide-react"
import { Button, Text } from "./ui"

/** Last line of defence: a render crash shows a recovery screen instead of a blank page. */
export default class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null }

  static getDerivedStateFromError(error: Error) {
    return { error }
  }

  componentDidCatch(error: Error) {
    console.error(error)
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div className="app">
        <main className="app-main">
          <div className="classwork-empty app-fallback" role="alert">
            <TriangleAlert size={32} />
            <Text as="h3">Đã có lỗi xảy ra</Text>
            <Text>Ứng dụng gặp sự cố ngoài ý muốn. Tải lại trang để tiếp tục.</Text>
            <Button variant="primary" onClick={() => window.location.reload()}><RotateCw size={16} />Tải lại</Button>
          </div>
        </main>
      </div>
    )
  }
}

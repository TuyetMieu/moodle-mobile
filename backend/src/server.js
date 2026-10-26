import { createServer } from "node:http"
import { networkInterfaces } from "node:os"
import { fileURLToPath } from "node:url"
import { createApp } from "./app.js"
import { isSeeded, openDatabase, resetDatabase, seededAt } from "./db.js"
import { DEMO_ACCOUNTS } from "./seed.js"

const PORT = Number(process.env.PORT ?? 8080)
const HOST = process.env.HOST ?? "0.0.0.0"
const DB_FILE = process.env.DB_FILE ?? fileURLToPath(new URL("../data/moodle-usth.db", import.meta.url))
const ALLOW_RESET = process.env.NODE_ENV !== "production"

const args = new Set(process.argv.slice(2))
const db = openDatabase(DB_FILE)

if (args.has("--reset") || args.has("--reset-only") || !isSeeded(db)) {
  resetDatabase(db)
  console.log(`Đã seed dữ liệu mẫu vào ${DB_FILE}`)
}
if (args.has("--reset-only")) process.exit(0)

const ageDays = (Date.now() - seededAt(db)) / 86_400_000
if (ageDays > 1) {
  console.warn(`⚠  Dữ liệu được seed ${ageDays.toFixed(1)} ngày trước nên nhiều deadline đã quá hạn. Chạy "npm run reset" để làm mới.`)
}

const app = createApp(db, { allowReset: ALLOW_RESET, log: (line) => console.log(line) })

createServer(app).listen(PORT, HOST, () => {
  const lanAddresses = Object.values(networkInterfaces()).flat()
    .filter((a) => a && a.family === "IPv4" && !a.internal).map((a) => a.address)
  console.log(`Moodle USTH API đang chạy:`)
  console.log(`  • Máy này:          http://localhost:${PORT}`)
  console.log(`  • Android emulator: http://10.0.2.2:${PORT}`)
  for (const address of lanAddresses) console.log(`  • Điện thoại (LAN):  http://${address}:${PORT}`)
  console.log(`Tài khoản demo: ${DEMO_ACCOUNTS.map((a) => `${a.username} / ${a.password}`).join(", ")}`)
})

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    db.close()
    process.exit(0)
  })
}

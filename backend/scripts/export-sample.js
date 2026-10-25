// Xuất payload /api/v1/bootstrap của tài khoản demo thành file JSON đóng gói trong app,
// dùng cho "Chế độ xem thử" khi không kết nối được backend. App tự dời mọi mốc thời gian
// theo `serverTime`, nên deadline trong file luôn tương đối so với lúc mở app.
import { writeFileSync } from "node:fs"
import { fileURLToPath } from "node:url"
import { openDatabase, resetDatabase } from "../src/db.js"
import { bootstrap } from "../src/services.js"

const target = fileURLToPath(new URL("../../app/src/main/assets/sample_bootstrap.json", import.meta.url))
const db = openDatabase(":memory:")
const now = Date.now()
resetDatabase(db, now)
const user = db.prepare("SELECT * FROM users WHERE username = 'ba22-041'").get()
writeFileSync(target, JSON.stringify(bootstrap(db, user, now), null, 2) + "\n")
console.log(`Đã ghi ${target}`)

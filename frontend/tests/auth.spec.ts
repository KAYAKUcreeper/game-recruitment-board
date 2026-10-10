import { test, expect, type Page } from "@playwright/test";
const password = "browser-test-passphrase-12345";
async function login(page: Page, id: string) {
  await page.goto("/login");
  await page.getByLabel("ログインID").fill(id);
  await page.getByLabel("パスワード").fill(password);
  await page.getByRole("button", { name: "ログインする" }).click();
  await expect(page.getByText("ブラウザ利用者さんでログイン中")).toBeVisible();
}
test("registration, shared tabs, independent devices and copied-token rejection", async ({
  page,
  browser,
  playwright,
}) => {
  const id = "e2e_" + Date.now();
  await page.goto("/register");
  await page.getByLabel("ログインID").fill(id);
  await page.getByLabel("パスワード").fill(password);
  await page.getByLabel("表示名").fill("ブラウザ利用者");
  await page.getByRole("button", { name: "登録する" }).click();
  await expect(page.getByText("登録完了")).toBeVisible();
  await login(page, id);
  expect(await page.evaluate(() => document.cookie)).not.toContain(
    "auth_token",
  );
  const oldState = await page.context().storageState();
  const tab = await page.context().newPage();
  await tab.goto("/login");
  await expect(tab.getByText("ブラウザ利用者さんでログイン中")).toBeVisible();
  const device = await browser.newContext();
  const other = await device.newPage();
  await login(other, id);
  await page.getByRole("button", { name: "ログアウト", exact: true }).click();
  await expect(page.getByText("ログインしていません。")).toBeVisible();
  await tab.reload();
  await expect(tab.getByText("ログインしていません。")).toBeVisible();
  await other.reload();
  await expect(other.getByText("ブラウザ利用者さんでログイン中")).toBeVisible();
  const copied = await playwright.request.newContext({
    baseURL: "http://localhost:5174",
    storageState: oldState,
  });
  expect((await copied.get("/api/auth/me")).status()).toBe(401);
  await copied.dispose();
  await login(page, id);
  await device.close();
});
test("duplicate ID is explained on registration screen", async ({ page }) => {
  const id = "dup_" + Date.now();
  for (let n = 0; n < 2; n++) {
    await page.goto("/register");
    await page.getByLabel("ログインID").fill(id);
    await page.getByLabel("パスワード").fill(password);
    await page.getByLabel("表示名").fill("ブラウザ利用者");
    await page.getByRole("button", { name: "登録する" }).click();
    if (n === 0) await expect(page.getByText("登録完了")).toBeVisible();
    else
      await expect(page.getByRole("alert")).toHaveText(
        "このログインIDは使用されています。",
      );
  }
});

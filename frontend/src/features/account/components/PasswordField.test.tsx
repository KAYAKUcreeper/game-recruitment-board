import { render, screen } from "@testing-library/react";
import { it, expect, vi } from "vitest";
import PasswordField from "./PasswordField";

it("disables password entry when masking is unsupported", () => {
  const supports = vi.spyOn(CSS, "supports").mockReturnValue(false);
  try {
    render(<PasswordField value="" onChange={() => {}} autoComplete="new-password" invalid={false} />);
    expect(screen.getByLabelText("パスワード")).toBeDisabled();
    expect(screen.getByText(/最新版のChromeまたはFirefox/)).toBeVisible();
    expect(screen.getByLabelText("パスワード")).toHaveClass("password-input");
  } finally {
    supports.mockRestore();
  }
});

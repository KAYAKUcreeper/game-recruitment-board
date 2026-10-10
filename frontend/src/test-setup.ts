import "@testing-library/jest-dom/vitest";
// jsdom has no CSS rendering; browser tests verify the actual masking support.
Object.defineProperty(CSS, "supports", {
  configurable: true,
  value: () => true,
});
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";
afterEach(cleanup);

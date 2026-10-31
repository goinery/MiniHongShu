import { brandName } from "@/config/brand";

export function installBrandFavicon() {
  const link = document.querySelector<HTMLLinkElement>("#brand-favicon");
  if (!link) return;

  const namespace = "http://www.w3.org/2000/svg";
  const svg = document.createElementNS(namespace, "svg");
  svg.setAttribute("viewBox", "0 0 64 64");

  const background = document.createElementNS(namespace, "rect");
  background.setAttribute("width", "64");
  background.setAttribute("height", "64");
  background.setAttribute("rx", "14");
  background.setAttribute(
    "fill",
    getComputedStyle(document.documentElement).getPropertyValue("--el-color-primary").trim()
  );

  const text = document.createElementNS(namespace, "text");
  text.textContent = Array.from(brandName).slice(0, 2).join("");
  text.setAttribute("x", "32");
  text.setAttribute("y", "33");
  text.setAttribute("dy", ".35em");
  text.setAttribute("text-anchor", "middle");
  text.setAttribute("font-family", "system-ui, sans-serif");
  text.setAttribute("font-size", "25");
  text.setAttribute("font-weight", "700");
  text.setAttribute("fill", "white");

  svg.append(background, text);
  link.href = `data:image/svg+xml,${encodeURIComponent(new XMLSerializer().serializeToString(svg))}`;
}

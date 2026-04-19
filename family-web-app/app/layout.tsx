import type { Metadata } from "next";
import { FamilyAppProvider } from "../components/family-app-provider";
import "./globals.css";

export const metadata: Metadata = {
  title: "小芳在家 - 子女端",
  description: "子女端移动 H5 骨架，优先适配微信内打开。",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>
        <FamilyAppProvider>{children}</FamilyAppProvider>
      </body>
    </html>
  );
}

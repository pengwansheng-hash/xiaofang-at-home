"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useFamilyApp } from "./family-app-provider";

type FamilyShellProps = {
  title: string;
  subtitle: string;
  children: React.ReactNode;
  tone?: "default" | "warn" | "danger";
  toneText?: string;
};

const navItems = [
  { href: "/today", label: "今日", icon: "今" },
  { href: "/reminders", label: "提醒", icon: "提" },
  { href: "/alerts", label: "异常", icon: "警" },
  { href: "/profile", label: "资料", icon: "人" },
];

export function FamilyShell({ title, subtitle, children, tone = "default", toneText }: FamilyShellProps) {
  const pathname = usePathname();
  const { binding } = useFamilyApp();

  return (
    <main className="app-shell">
      <div className="page-stack">
        <header className="hero">
          <span className="hero__eyebrow">
            小芳在家 · 子女端
            {binding.isBound ? ` · 已连接 ${binding.elderName}` : " · 还未绑定"}
          </span>
          <h1 className="hero__title">{title}</h1>
          <p className="hero__subtitle">{subtitle}</p>
          {tone !== "default" && toneText ? (
            <div className={`risk-banner risk-banner--${tone}`}>{toneText}</div>
          ) : null}
        </header>
        {children}
      </div>

      <nav className="sticky-nav" aria-label="子女端导航">
        {navItems.map((item) => {
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`sticky-nav__item${active ? " sticky-nav__item--active" : ""}`}
            >
              <span className="sticky-nav__icon">{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </main>
  );
}

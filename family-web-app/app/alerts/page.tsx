"use client";

import Link from "next/link";
import { FamilyShell } from "../../components/family-shell";
import { useFamilyApp } from "../../components/family-app-provider";
import { NoticeCard, SectionTitle } from "../../components/ui";
import type { FamilyAlertAction } from "../../lib/mock-family-data";

function toneClass(level: string) {
  if (level.includes("失败") || level.includes("今晚") || level.includes("高")) return "pill--danger";
  if (level.includes("同步") || level.includes("待") || level.includes("占位")) return "pill--warn";
  return "pill--good";
}

function renderAction(action: FamilyAlertAction, onDismiss: () => void, secondary = false) {
  const className = secondary ? "btn-secondary" : "btn";
  const buttonStyle = { display: "inline-flex", alignItems: "center", justifyContent: "center" } as const;

  if (action.kind === "link") {
    return (
      <Link href={action.href} className={className} style={buttonStyle}>
        {action.label}
      </Link>
    );
  }

  if (action.kind === "tel") {
    return (
      <a href={`tel:${action.phone}`} className={className} style={buttonStyle}>
        {action.label}
      </a>
    );
  }

  return (
    <button type="button" className={className} onClick={onDismiss}>
      {action.label}
    </button>
  );
}

export default function AlertsPage() {
  const { binding, alerts, dismissAlert } = useFamilyApp();
  const topAlert = alerts[0];
  const isBindingFailed = binding.validationStatus === "校验失败";

  return (
    <FamilyShell
      title={alerts.length > 0 ? "先看最值得介入的事" : "今天先不用急"}
      subtitle={
        alerts.length > 0
          ? isBindingFailed
            ? "当前最靠前的是绑定失败引导，先把绑定状态处理对，再看提醒和异常。"
            : "这里只放你现在值得做动作的事，先处理最靠前的一张。"
          : binding.isBound
            ? "当前没有需要你立刻介入的异常，保持低频关注就好。"
            : "还没连上老人端，现在这页先告诉你第一步该做什么。"
      }
      tone={alerts.length > 0 ? (isBindingFailed ? "danger" : "warn") : "default"}
      toneText={alerts.length > 0 ? topAlert.action : undefined}
    >
      {alerts.length === 0 ? (
        <NoticeCard title="今天先不用急" body="这会儿没有需要你立刻介入的异常，先看今日状态或提醒设置就够了。" />
      ) : null}

      {alerts.length > 0 ? (
        <section className="card">
          <SectionTitle title="异常摘要" meta={`${alerts.length} 条`} />
          <div className="list" style={{ marginTop: 14 }}>
            {alerts.map((alert) => (
              <article key={alert.id} className="list-row">
                <div className="list-row__top">
                  <div>
                    <h3 className="list-row__title">{alert.reason}</h3>
                    <p className="list-row__desc">{alert.source}</p>
                  </div>
                  <span className={`pill ${toneClass(alert.level)}`}>{alert.level}</span>
                </div>
                <p className="list-row__desc">{alert.action}</p>
                <div className="action-row">
                  {renderAction(alert.primaryAction, () => dismissAlert(alert.id))}
                  {alert.secondaryAction
                    ? renderAction(alert.secondaryAction, () => dismissAlert(alert.id), true)
                    : null}
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : null}

      <section className="card card--soft">
        <SectionTitle title="这一页怎么来的" meta="首版本地联动" />
        <p className="list-row__desc" style={{ marginTop: 12 }}>
          异常页现在会根据绑定状态、提醒待同步情况和待确认提醒，自动给出建议动作，不再只是固定演示文案。
        </p>
        {isBindingFailed ? (
          <div className="action-row" style={{ marginTop: 14 }}>
            <Link
              href="/bind"
              className="btn"
              style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
            >
              先回绑定页
            </Link>
          </div>
        ) : null}
      </section>
    </FamilyShell>
  );
}

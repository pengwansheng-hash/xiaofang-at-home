"use client";

import Link from "next/link";
import { FamilyShell } from "../../components/family-shell";
import { useFamilyApp } from "../../components/family-app-provider";
import { MetricCard, NoticeCard, SectionTitle } from "../../components/ui";
import { getBindingStatusMeta, memoryNotes } from "../../lib/mock-family-data";

export default function TodayPage() {
  const { binding, profile, contacts, reminders, todaySummary, alerts } = useFamilyApp();
  const topAlert = alerts[0];
  const validationMeta = getBindingStatusMeta(binding.validationStatus);

  const heroSubtitle = !binding.isBound
    ? "先把老人端连上，今日结论和异常才有真实家庭上下文。"
    : binding.validationStatus === "校验失败"
      ? "这次绑定还没通过校验，先回绑定页重试，再决定今天要不要继续介入。"
      : topAlert
        ? `现在最值得先处理的是“${topAlert.reason}”。`
        : todaySummary.summary;

  const tone = binding.validationStatus === "校验失败" || todaySummary.pendingSyncCount > 0 ? "danger" : topAlert ? "warn" : "default";
  const toneText = binding.validationStatus === "校验失败" ? "先回绑定页重试，避免按错误家庭上下文继续处理。" : topAlert ? topAlert.action : todaySummary.actionHint;

  return (
    <FamilyShell
      title={binding.isBound ? `${binding.elderName}今天先不用急` : "先绑定再看今天状态"}
      subtitle={heroSubtitle}
      tone={tone}
      toneText={toneText}
    >
      {!binding.isBound ? (
        <NoticeCard
          title="还没连上老人端"
          body="当前看到的是示例骨架。先去绑定页输入 6 位绑定码，后面这页才会进入真实家庭上下文。"
        />
      ) : null}

      {binding.isBound ? (
        <section className="card card--soft">
          <SectionTitle title="当前家庭上下文" meta="绑定成功流联动" />
          <div className="list" style={{ marginTop: 14 }}>
            <div className="list-row">
              <div className="list-row__top">
                <div>
                  <h3 className="list-row__title">
                    正在看 {binding.elderName} 的今天状态
                  </h3>
                  <p className="list-row__desc">
                    当前关系是{profile.relation}，绑定于 {binding.lastBoundAt ?? "刚刚"}，最近作息重点是“{profile.routine}”
                  </p>
                </div>
                <span className={validationMeta.pillClassName}>{validationMeta.label}</span>
              </div>
              <div className="badge-row">
                <span className="pill">{binding.bindingMethod}</span>
                <span className="pill">{contacts.length} 位联系人</span>
                <span className="pill">{reminders.length} 条提醒</span>
                {todaySummary.pendingSyncCount > 0 ? <span className="pill pill--danger">有待同步变更</span> : null}
              </div>
              <p className="list-row__desc">{binding.validationNote}</p>
              <div className="action-row">
                <Link
                  href={binding.validationStatus === "校验失败" ? "/bind" : "/reminders"}
                  className="btn-secondary"
                  style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
                >
                  {binding.validationStatus === "校验失败" ? "先回绑定页" : "先去看提醒"}
                </Link>
                <Link
                  href="/profile"
                  className="btn-secondary"
                  style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
                >
                  去看资料
                </Link>
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="metric-grid">
        <MetricCard
          label="今天提醒"
          value={`${todaySummary.totalCount}`}
          hint={todaySummary.totalCount > 0 ? "今天要照看的提醒数量" : "现在还没配提醒"}
        />
        <MetricCard
          label="待同步"
          value={`${todaySummary.pendingSyncCount}`}
          hint={todaySummary.pendingSyncCount > 0 ? "这些变更还没回到老人端" : "当前没有待同步变更"}
        />
        <MetricCard
          label="待介入"
          value={`${alerts.length}`}
          hint={alerts.length > 0 ? "先看异常页里最靠前的一条" : "当前没有需要你立刻介入的异常"}
        />
      </section>

      <section className="card">
        <SectionTitle title="今天结论" meta={binding.validationStatus === "校验失败" ? "先回绑" : topAlert ? topAlert.level : todaySummary.riskLevel} />
        <p className="hero__subtitle" style={{ marginTop: 12 }}>
          {!binding.isBound
            ? "先完成绑定，再决定今天是不是需要介入。"
            : binding.validationStatus === "校验失败"
              ? "先别按当前家庭上下文继续推进，回绑定页重新确认之后，再看今天状态和异常。"
              : topAlert
                ? `先去异常页看“${topAlert.reason}”，再决定今晚是不是需要继续打扰。`
                : `今天最值得盯住的是 ${todaySummary.nextReminderTime} 的“${todaySummary.nextReminderTitle}”。`}
        </p>
      </section>

      <section className="card card--soft">
        <SectionTitle title="刚改过的提醒会同步到这里" meta={`${todaySummary.syncedCount} 条已稳定`} />
        <p className="list-row__desc" style={{ marginTop: 12 }}>
          你在提醒页刚新增或改过的内容，这里会马上反映出来，避免“提醒改了，首页还没变”。
        </p>
        <div className="badge-row" style={{ marginTop: 12 }}>
          <span className="pill">{todaySummary.nextReminderTime}</span>
          <span className="pill">{todaySummary.nextReminderTitle}</span>
          {todaySummary.pendingSyncCount > 0 ? <span className="pill pill--danger">有待同步变更</span> : null}
        </div>
      </section>

      <section className="card">
        <SectionTitle title="最近记忆" meta="近 3 天" />
        <div className="list" style={{ marginTop: 12 }}>
          {memoryNotes.map((note) => (
            <div key={note} className="list-row">
              <p className="list-row__desc">{note}</p>
            </div>
          ))}
        </div>
      </section>

      <NoticeCard title="今晚最值得做的动作" body={toneText} />
      {!binding.isBound ? (
        <Link
          href="/bind"
          className="btn"
          style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
        >
          现在去绑定
        </Link>
      ) : null}
    </FamilyShell>
  );
}

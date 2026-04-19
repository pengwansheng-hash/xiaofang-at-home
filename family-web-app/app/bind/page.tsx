"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { FamilyShell } from "../../components/family-shell";
import { useFamilyApp } from "../../components/family-app-provider";
import { NoticeCard, SectionTitle } from "../../components/ui";
import { getBindingStatusMeta } from "../../lib/mock-family-data";

function getValidationActionCopy(status: ReturnType<typeof getBindingStatusMeta>["label"]) {
  switch (status) {
    case "校验通过":
      return "当前已经切到“校验通过”占位结果，后续可以继续补提醒和资料。";
    case "校验失败":
      return "当前已经切到“校验失败”占位结果，用来验收异常页和今日页的回绑引导。";
    case "待服务端校验":
      return "当前回到“待服务端校验”状态，仍然保留本地绑定成功流。";
    default:
      return "";
  }
}

export default function BindPage() {
  const [bindingCode, setBindingCode] = useState("426318");
  const [formHint, setFormHint] = useState("");
  const [scanHint, setScanHint] = useState("");
  const { binding, profile, contacts, reminders, bindFamily, updateBindingValidation, clearBinding } = useFamilyApp();

  const validationMeta = getBindingStatusMeta(binding.validationStatus);
  const isCodeReady = bindingCode.length === 6;
  const nextStepText = useMemo(() => {
    if (binding.validationStatus === "校验失败") {
      return "先回头确认绑定码，再看今天状态。";
    }

    if (reminders.length === 0) {
      return "先去提醒页补 1 到 2 条最关键的提醒。";
    }

    return "先看今天状态，再把提醒和资料补完整。";
  }, [binding.validationStatus, reminders.length]);

  return (
    <FamilyShell title="先把妈妈连上" subtitle="输入她手机上的 6 位绑定码，先把今天的状态看起来。">
      <section className="card card--soft">
        <div className="field">
          <label htmlFor="binding-code">绑定码</label>
          <input
            id="binding-code"
            className="input"
            inputMode="numeric"
            maxLength={6}
            value={bindingCode}
            onChange={(event) => {
              setBindingCode(event.target.value.replace(/\D/g, "").slice(0, 6));
              setFormHint("");
            }}
            placeholder="请输入 6 位绑定码"
          />
        </div>
        <div className="badge-row" style={{ marginTop: 12 }}>
          <span className="pill">{isCodeReady ? "已满足 6 位" : `还差 ${6 - bindingCode.length} 位`}</span>
          <span className="pill">首版先跑本地绑定成功流</span>
        </div>
        <div className="action-row" style={{ marginTop: 14 }}>
          <button
            type="button"
            className="btn btn--full"
            disabled={!isCodeReady}
            onClick={() => {
              if (!isCodeReady) {
                setFormHint("先把 6 位绑定码输完整，再继续。");
                return;
              }
              bindFamily(bindingCode, profile.name, "手动输入");
              setFormHint("已经按手动输入方式连上，下一步可以继续验收绑定状态和跨页联动。");
              setScanHint("");
            }}
          >
            绑定并查看今天状态
          </button>
          <button
            type="button"
            className="btn-secondary btn--full"
            onClick={() => {
              const previewCode = "593204";
              setBindingCode(previewCode);
              bindFamily(previewCode, profile.name, "扫码导入");
              setFormHint("");
              setScanHint("已带回示例绑定码，当前仍是扫码导入的本地占位流程，后面再接真实扫码校验。");
            }}
          >
            微信扫码占位
          </button>
        </div>
      </section>

      {formHint ? <NoticeCard title="绑定提示" body={formHint} /> : null}
      {scanHint ? <NoticeCard title="扫码占位反馈" body={scanHint} /> : null}

      {binding.isBound ? (
        <>
          <NoticeCard
            title={binding.validationStatus === "校验失败" ? "绑定成功流已切到失败态" : "已带回当前家庭上下文"}
            body={`已经把 ${binding.elderName} 连上了，绑定时间 ${binding.lastBoundAt ?? "刚刚"}。这一步先把绑定成功流、校验状态和后续入口都看顺。`}
          />

          <section className="card">
            <SectionTitle title="当前家庭上下文" meta="绑定成功流收尾" />
            <div className="list" style={{ marginTop: 14 }}>
              <div className="list-row">
                <div className="list-row__top">
                  <div>
                    <h3 className="list-row__title">{binding.elderName}</h3>
                    <p className="list-row__desc">
                      你当前连接的是{profile.relation}，最近作息重点是“{profile.routine}”
                    </p>
                  </div>
                  <span className={validationMeta.pillClassName}>{validationMeta.label}</span>
                </div>
                <div className="badge-row">
                  <span className="pill">绑定码 {binding.bindingCode}</span>
                  <span className="pill">{binding.bindingMethod}</span>
                  <span className="pill">{contacts.length} 位联系人</span>
                  <span className="pill">{reminders.length} 条提醒</span>
                </div>
                <p className="list-row__desc">{nextStepText}</p>
              </div>
            </div>
          </section>

          <section className="card card--soft">
            <SectionTitle title="当前校验状态" meta={binding.validationUpdatedAt ?? "刚刚更新"} />
            <p className="list-row__desc" style={{ marginTop: 12 }}>
              {binding.validationNote}
            </p>
            <p className="list-row__desc" style={{ marginTop: 8 }}>
              {validationMeta.summary}
            </p>
            <div className="action-row" style={{ marginTop: 14 }}>
              <button
                type="button"
                className="btn"
                onClick={() => {
                  updateBindingValidation("校验通过", "当前通过本地验收按钮模拟了“校验通过”，后续再接真实服务端校验。");
                  setFormHint(getValidationActionCopy("校验通过"));
                }}
              >
                模拟校验通过
              </button>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  updateBindingValidation("校验失败", "当前通过本地验收按钮模拟了“校验失败”，用于验收回绑定引导。");
                  setFormHint(getValidationActionCopy("校验失败"));
                }}
              >
                模拟校验失败
              </button>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  updateBindingValidation("待服务端校验", "已恢复为待服务端校验状态，当前仍是本地绑定成功流。");
                  setFormHint(getValidationActionCopy("待服务端校验"));
                }}
              >
                回到待校验
              </button>
            </div>
          </section>

          <section className="card">
            <SectionTitle title="下一步最自然的动作" meta="继续收尾 FAM-02" />
            <div className="action-row" style={{ marginTop: 14 }}>
              <Link
                href="/today"
                className="btn"
                style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
              >
                去看今天状态
              </Link>
              <Link
                href="/reminders"
                className="btn-secondary"
                style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
              >
                去补提醒
              </Link>
              <Link
                href="/profile"
                className="btn-secondary"
                style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
              >
                去看资料
              </Link>
            </div>
          </section>

          <button type="button" className="btn-secondary" onClick={clearBinding}>
            清除当前绑定占位
          </button>
        </>
      ) : (
        <NoticeCard
          title="现在先做最小闭环"
          body="首版先跑通绑定码输入、绑定成功流和跨页联动；扫码和服务端正式校验下一段再接上。"
        />
      )}

      <Link
        href="/today"
        className="btn-secondary"
        style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
      >
        先看示例状态页
      </Link>
    </FamilyShell>
  );
}

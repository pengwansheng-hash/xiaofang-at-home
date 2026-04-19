"use client";

import { useMemo, useState } from "react";
import { FamilyShell } from "../../components/family-shell";
import { useFamilyApp } from "../../components/family-app-provider";
import { NoticeCard, SectionTitle } from "../../components/ui";

type ReminderFormState = {
  id?: string;
  title: string;
  time: string;
  frequency: string;
  channel: string;
};

const defaultForm: ReminderFormState = {
  title: "",
  time: "08:00",
  frequency: "每天",
  channel: "语音播报 + 铃声",
};

export default function RemindersPage() {
  const { reminders, saveReminder, deleteReminder } = useFamilyApp();
  const [editing, setEditing] = useState<ReminderFormState | null>(null);

  const reminderCountText = useMemo(() => `${reminders.length} 项`, [reminders.length]);

  const openNewForm = () => setEditing(defaultForm);

  const openEditForm = (id: string) => {
    const target = reminders.find((item) => item.id === id);
    if (!target) return;
    setEditing({
      id: target.id,
      title: target.title,
      time: target.time,
      frequency: target.frequency,
      channel: target.channel,
    });
  };

  const updateField = (key: keyof ReminderFormState, value: string) => {
    setEditing((current) => {
      if (!current) return current;
      return { ...current, [key]: value };
    });
  };

  const handleSave = () => {
    if (!editing) return;
    if (!editing.title.trim()) return;
    saveReminder({
      id: editing.id,
      title: editing.title.trim(),
      time: editing.time,
      frequency: editing.frequency,
      channel: editing.channel,
    });
    setEditing(null);
  };

  const handleDelete = (id: string) => {
    if (editing?.id === id) {
      setEditing(null);
    }
    deleteReminder(id);
  };

  return (
    <FamilyShell title="提醒先配少一点" subtitle="先把每天最关键的几件事配稳，再慢慢补更多规则。">
      {editing ? (
        <section className="card card--soft">
          <SectionTitle title={editing.id ? "修改提醒" : "新增提醒"} meta="FAM-04" />
          <div className="field-grid" style={{ marginTop: 14 }}>
            <div className="field">
              <label htmlFor="title">提醒内容</label>
              <input
                id="title"
                className="input"
                value={editing.title}
                onChange={(event) => updateField("title", event.target.value)}
                placeholder="例如：午饭后喝水"
              />
            </div>
            <div className="field">
              <label htmlFor="time">提醒时间</label>
              <input
                id="time"
                className="input"
                type="time"
                value={editing.time}
                onChange={(event) => updateField("time", event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="frequency">频率</label>
              <select
                id="frequency"
                className="input"
                value={editing.frequency}
                onChange={(event) => updateField("frequency", event.target.value)}
              >
                <option value="每天">每天</option>
                <option value="周一、周三、周五">周一、周三、周五</option>
                <option value="单次提醒">单次提醒</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="channel">提醒方式</label>
              <select
                id="channel"
                className="input"
                value={editing.channel}
                onChange={(event) => updateField("channel", event.target.value)}
              >
                <option value="语音播报 + 铃声">语音播报 + 铃声</option>
                <option value="语音播报">语音播报</option>
                <option value="铃声">铃声</option>
              </select>
            </div>
          </div>
          <div className="action-row" style={{ marginTop: 16 }}>
            <button type="button" className="btn" onClick={handleSave}>
              保存这条提醒
            </button>
            <button type="button" className="btn-secondary" onClick={() => setEditing(null)}>
              先取消
            </button>
          </div>
        </section>
      ) : (
        <NoticeCard title="先把计划配稳" body="首版先支持在本地新增和修改提醒，和老人端同步这一步下一段接上。" />
      )}

      <section className="card">
        <SectionTitle title="提醒计划" meta={reminderCountText} />
        <div className="list" style={{ marginTop: 14 }}>
          {reminders.map((plan) => (
            <article key={plan.id} className="list-row">
              <div className="list-row__top">
                <div>
                  <h3 className="list-row__title">{plan.title}</h3>
                  <p className="list-row__desc">{plan.frequency}</p>
                </div>
                <span className="pill pill--good">{plan.status}</span>
              </div>
              <div className="list-row__bottom">
                <div className="badge-row">
                  <span className="pill">{plan.time}</span>
                  <span className="pill">{plan.channel}</span>
                </div>
                <div className="action-row">
                  <button type="button" className="btn-secondary" onClick={() => openEditForm(plan.id)}>
                    修改
                  </button>
                  <button type="button" className="btn-secondary" onClick={() => handleDelete(plan.id)}>
                    删除
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="card card--soft">
        <SectionTitle title="下一步" meta="本地骨架已可增改" />
        <p className="list-row__desc" style={{ marginTop: 12 }}>
          现在已经能在浏览器里把提醒计划记住了，接下来把绑定成功流和提醒同步结果页继续接起来。
        </p>
        <div className="action-row" style={{ marginTop: 12 }}>
          <button type="button" className="btn" onClick={openNewForm}>
            新增提醒
          </button>
        </div>
      </section>
    </FamilyShell>
  );
}

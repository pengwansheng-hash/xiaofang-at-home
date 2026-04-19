"use client";

import { useState } from "react";
import { FamilyShell } from "../../components/family-shell";
import { useFamilyApp } from "../../components/family-app-provider";
import { NoticeCard, SectionTitle } from "../../components/ui";
import type { FamilyContact, FamilyProfileState } from "../../lib/mock-family-data";

function toTextareaValue(items: string[]) {
  return items.join("，");
}

function toList(value: string) {
  return value
    .split(/[，,、\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export default function ProfilePage() {
  const { profile, contacts, saveProfile, saveContact } = useFamilyApp();
  const [editing, setEditing] = useState<FamilyProfileState>(profile);
  const [contactEditing, setContactEditing] = useState<Omit<FamilyContact, "id"> & { id?: string }>({
    name: "",
    relation: "",
    phone: "",
  });

  const handleSave = () => {
    saveProfile({
      ...editing,
      commonTopics: toList(toTextareaValue(editing.commonTopics)),
      tabooWords: toList(toTextareaValue(editing.tabooWords)),
    });
  };

  const handleSaveContact = () => {
    if (!contactEditing.name.trim() || !contactEditing.phone.trim()) {
      return;
    }
    saveContact({
      id: contactEditing.id,
      name: contactEditing.name.trim(),
      relation: contactEditing.relation.trim() || "家里人",
      phone: contactEditing.phone.trim(),
    });
    setContactEditing({ name: "", relation: "", phone: "" });
  };

  return (
    <FamilyShell title="资料先抓住熟悉感" subtitle="先把称呼、话题和禁忌词记稳，后面再接服务端同步。">
      <section className="card card--soft">
        <SectionTitle title="本地编辑资料" meta="FAM-05" />
        <div className="field-grid" style={{ marginTop: 14 }}>
          <div className="field">
            <label htmlFor="greeting-style">沟通风格</label>
            <input
              id="greeting-style"
              className="input"
              value={editing.greetingStyle}
              onChange={(event) => setEditing((current) => ({ ...current, greetingStyle: event.target.value }))}
              placeholder="例如：温和陪伴"
            />
          </div>
          <div className="field">
            <label htmlFor="relation">关系称呼</label>
            <input
              id="relation"
              className="input"
              value={editing.relation}
              onChange={(event) => setEditing((current) => ({ ...current, relation: event.target.value }))}
              placeholder="例如：妈妈"
            />
          </div>
          <div className="field" style={{ gridColumn: "1 / -1" }}>
            <label htmlFor="common-topics">常聊话题</label>
            <textarea
              id="common-topics"
              className="textarea"
              value={toTextareaValue(editing.commonTopics)}
              onChange={(event) =>
                setEditing((current) => ({
                  ...current,
                  commonTopics: toList(event.target.value),
                }))
              }
              placeholder="例如：早餐，花草，邻里聊天"
            />
          </div>
          <div className="field" style={{ gridColumn: "1 / -1" }}>
            <label htmlFor="taboo-words">尽量避开</label>
            <textarea
              id="taboo-words"
              className="textarea"
              value={toTextareaValue(editing.tabooWords)}
              onChange={(event) =>
                setEditing((current) => ({
                  ...current,
                  tabooWords: toList(event.target.value),
                }))
              }
              placeholder="例如：别催，别唠叨"
            />
          </div>
          <div className="field" style={{ gridColumn: "1 / -1" }}>
            <label htmlFor="routine">作息提醒</label>
            <textarea
              id="routine"
              className="textarea"
              value={editing.routine}
              onChange={(event) => setEditing((current) => ({ ...current, routine: event.target.value }))}
              placeholder="例如：早上 7:30 起床，午饭前后最容易忘记喝水。"
            />
          </div>
        </div>
        <div className="action-row" style={{ marginTop: 16 }}>
          <button type="button" className="btn" onClick={handleSave}>
            保存这些资料
          </button>
          <button type="button" className="btn-secondary" onClick={() => setEditing(profile)}>
            恢复当前版本
          </button>
        </div>
      </section>

      <section className="card card--soft">
        <SectionTitle title={contactEditing.id ? "修改联系人" : "新增联系人"} meta="联系人本地编辑" />
        <div className="field-grid" style={{ marginTop: 14 }}>
          <div className="field">
            <label htmlFor="contact-name">联系人姓名</label>
            <input
              id="contact-name"
              className="input"
              value={contactEditing.name}
              onChange={(event) => setContactEditing((current) => ({ ...current, name: event.target.value }))}
              placeholder="例如：李叔叔"
            />
          </div>
          <div className="field">
            <label htmlFor="contact-relation">关系</label>
            <input
              id="contact-relation"
              className="input"
              value={contactEditing.relation}
              onChange={(event) => setContactEditing((current) => ({ ...current, relation: event.target.value }))}
              placeholder="例如：邻居"
            />
          </div>
          <div className="field" style={{ gridColumn: "1 / -1" }}>
            <label htmlFor="contact-phone">手机号</label>
            <input
              id="contact-phone"
              className="input"
              inputMode="tel"
              value={contactEditing.phone}
              onChange={(event) =>
                setContactEditing((current) => ({
                  ...current,
                  phone: event.target.value.replace(/[^\d]/g, "").slice(0, 11),
                }))
              }
              placeholder="例如：13800005678"
            />
          </div>
        </div>
        <div className="action-row" style={{ marginTop: 16 }}>
          <button type="button" className="btn" onClick={handleSaveContact}>
            保存联系人
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={() => setContactEditing({ name: "", relation: "", phone: "" })}
          >
            清空这张表
          </button>
        </div>
      </section>

      <section className="card">
        <SectionTitle title={`${profile.relation}的资料`} meta={profile.greetingStyle} />
        <div className="list" style={{ marginTop: 14 }}>
          <div className="list-row">
            <div className="list-row__top">
              <h3 className="list-row__title">常聊话题</h3>
            </div>
            <div className="badge-row">
              {profile.commonTopics.map((topic) => (
                <span key={topic} className="pill">
                  {topic}
                </span>
              ))}
            </div>
          </div>
          <div className="list-row">
            <div className="list-row__top">
              <h3 className="list-row__title">尽量避开</h3>
            </div>
            <div className="badge-row">
              {profile.tabooWords.map((word) => (
                <span key={word} className="pill pill--warn">
                  {word}
                </span>
              ))}
            </div>
          </div>
          <div className="list-row">
            <h3 className="list-row__title">作息提醒</h3>
            <p className="list-row__desc">{profile.routine}</p>
          </div>
        </div>
      </section>

      <section className="card">
        <SectionTitle title="重要联系人" meta={`${contacts.length} 位`} />
        <div className="list" style={{ marginTop: 14 }}>
          {contacts.map((contact) => (
            <article key={contact.id} className="list-row">
              <div className="list-row__top">
                <div>
                  <h3 className="list-row__title">{contact.name}</h3>
                  <p className="list-row__desc">{contact.relation}</p>
                </div>
                <a
                  href={`tel:${contact.phone}`}
                  className="pill"
                  style={{ display: "inline-flex", alignItems: "center", justifyContent: "center" }}
                >
                  {contact.phone}
                </a>
              </div>
              <div className="action-row">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() =>
                    setContactEditing({
                      id: contact.id,
                      name: contact.name,
                      relation: contact.relation,
                      phone: contact.phone,
                    })
                  }
                >
                  修改
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <NoticeCard title="这一页先做成可维护" body="首版先把资料和联系人改动留在当前浏览器里，后面再接家庭同步和更完整的联系人管理。" />
    </FamilyShell>
  );
}

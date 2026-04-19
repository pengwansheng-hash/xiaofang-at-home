"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import {
  buildFamilyAlerts,
  buildFamilyTodaySummary,
  defaultContacts,
  defaultElderProfile,
  defaultReminderPlans,
  type FamilyAlertItem,
  type FamilyBindingState,
  type FamilyContact,
  type FamilyProfileState,
  type FamilyReminderPlan,
  type FamilyTodaySummary,
} from "../lib/mock-family-data";

type ReminderDraftInput = {
  id?: string;
  title: string;
  time: string;
  frequency: string;
  channel: string;
};

type FamilyAppContextValue = {
  binding: FamilyBindingState;
  profile: FamilyProfileState;
  contacts: FamilyContact[];
  reminders: FamilyReminderPlan[];
  todaySummary: FamilyTodaySummary;
  alerts: FamilyAlertItem[];
  bindFamily: (
    bindingCode: string,
    elderName: string,
    bindingMethod?: FamilyBindingState["bindingMethod"],
  ) => void;
  updateBindingValidation: (
    validationStatus: Extract<FamilyBindingState["validationStatus"], "待服务端校验" | "校验通过" | "校验失败">,
    validationNote: string,
  ) => void;
  clearBinding: () => void;
  saveProfile: (draft: FamilyProfileState) => void;
  saveContact: (draft: Omit<FamilyContact, "id"> & { id?: string }) => void;
  saveReminder: (draft: ReminderDraftInput) => void;
  deleteReminder: (id: string) => void;
  dismissAlert: (id: string) => void;
};

const BINDING_STORAGE_KEY = "xiaofang-family-binding";
const PROFILE_STORAGE_KEY = "xiaofang-family-profile";
const CONTACTS_STORAGE_KEY = "xiaofang-family-contacts";
const REMINDER_STORAGE_KEY = "xiaofang-family-reminders";
const DISMISSED_ALERTS_STORAGE_KEY = "xiaofang-family-dismissed-alerts";

const defaultBindingState: FamilyBindingState = {
  isBound: false,
  bindingCode: "",
  elderName: "张阿姨",
  lastBoundAt: null,
  bindingMethod: "手动输入",
  validationStatus: "本地占位",
  validationNote: "还没开始绑定，当前只是家属端骨架。",
  validationUpdatedAt: null,
};

const FamilyAppContext = createContext<FamilyAppContextValue | null>(null);

export function FamilyAppProvider({ children }: { children: React.ReactNode }) {
  const [binding, setBinding] = useState<FamilyBindingState>(defaultBindingState);
  const [profile, setProfile] = useState<FamilyProfileState>(defaultElderProfile);
  const [contacts, setContacts] = useState<FamilyContact[]>(defaultContacts);
  const [reminders, setReminders] = useState<FamilyReminderPlan[]>(defaultReminderPlans);
  const [dismissedAlerts, setDismissedAlerts] = useState<string[]>([]);

  useEffect(() => {
    const rawBinding = window.localStorage.getItem(BINDING_STORAGE_KEY);
    if (rawBinding) {
      try {
        const parsedBinding = JSON.parse(rawBinding) as Partial<FamilyBindingState>;
        setBinding({
          ...defaultBindingState,
          ...parsedBinding,
          validationNote:
            parsedBinding.validationNote ??
            (parsedBinding.isBound ? "当前是旧版本绑定记录，默认按待服务端校验处理。" : defaultBindingState.validationNote),
          validationUpdatedAt: parsedBinding.validationUpdatedAt ?? parsedBinding.lastBoundAt ?? null,
          validationStatus:
            parsedBinding.validationStatus ??
            (parsedBinding.isBound ? "待服务端校验" : defaultBindingState.validationStatus),
        });
      } catch {
        window.localStorage.removeItem(BINDING_STORAGE_KEY);
      }
    }

    const rawProfile = window.localStorage.getItem(PROFILE_STORAGE_KEY);
    if (rawProfile) {
      try {
        setProfile(JSON.parse(rawProfile) as FamilyProfileState);
      } catch {
        window.localStorage.removeItem(PROFILE_STORAGE_KEY);
      }
    }

    const rawContacts = window.localStorage.getItem(CONTACTS_STORAGE_KEY);
    if (rawContacts) {
      try {
        const parsedContacts = JSON.parse(rawContacts) as FamilyContact[];
        if (Array.isArray(parsedContacts) && parsedContacts.length > 0) {
          setContacts(parsedContacts);
        }
      } catch {
        window.localStorage.removeItem(CONTACTS_STORAGE_KEY);
      }
    }

    const rawReminders = window.localStorage.getItem(REMINDER_STORAGE_KEY);
    if (rawReminders) {
      try {
        const parsedReminders = JSON.parse(rawReminders) as FamilyReminderPlan[];
        if (Array.isArray(parsedReminders) && parsedReminders.length > 0) {
          setReminders(parsedReminders);
        }
      } catch {
        window.localStorage.removeItem(REMINDER_STORAGE_KEY);
      }
    }

    const rawDismissedAlerts = window.localStorage.getItem(DISMISSED_ALERTS_STORAGE_KEY);
    if (rawDismissedAlerts) {
      try {
        const parsedDismissedAlerts = JSON.parse(rawDismissedAlerts) as string[];
        if (Array.isArray(parsedDismissedAlerts)) {
          setDismissedAlerts(parsedDismissedAlerts);
        }
      } catch {
        window.localStorage.removeItem(DISMISSED_ALERTS_STORAGE_KEY);
      }
    }
  }, []);

  const resetDismissedAlerts = () => {
    setDismissedAlerts([]);
    window.localStorage.removeItem(DISMISSED_ALERTS_STORAGE_KEY);
  };

  const bindFamily = (
    bindingCode: string,
    elderName: string,
    bindingMethod: FamilyBindingState["bindingMethod"] = "手动输入",
  ) => {
    const nextState: FamilyBindingState = {
      isBound: true,
      bindingCode,
      elderName,
      lastBoundAt: new Date().toLocaleString("zh-CN", { hour12: false }),
      bindingMethod,
      validationStatus: "待服务端校验",
      validationNote:
        bindingMethod === "扫码导入"
          ? "已带回扫码占位结果，后续还要接真实扫码校验接口。"
          : "当前是手动输入绑定码的本地成功流，后续还要接服务端正式校验。",
      validationUpdatedAt: new Date().toLocaleString("zh-CN", { hour12: false }),
    };
    setBinding(nextState);
    window.localStorage.setItem(BINDING_STORAGE_KEY, JSON.stringify(nextState));
    resetDismissedAlerts();
  };

  const updateBindingValidation = (
    validationStatus: Extract<FamilyBindingState["validationStatus"], "待服务端校验" | "校验通过" | "校验失败">,
    validationNote: string,
  ) => {
    setBinding((current) => {
      if (!current.isBound) {
        return current;
      }

      const nextState: FamilyBindingState = {
        ...current,
        validationStatus,
        validationNote,
        validationUpdatedAt: new Date().toLocaleString("zh-CN", { hour12: false }),
      };

      window.localStorage.setItem(BINDING_STORAGE_KEY, JSON.stringify(nextState));
      return nextState;
    });
    resetDismissedAlerts();
  };

  const clearBinding = () => {
    setBinding(defaultBindingState);
    window.localStorage.removeItem(BINDING_STORAGE_KEY);
    resetDismissedAlerts();
  };

  const saveProfile = (draft: FamilyProfileState) => {
    setProfile(draft);
    window.localStorage.setItem(PROFILE_STORAGE_KEY, JSON.stringify(draft));
  };

  const saveContact = (draft: Omit<FamilyContact, "id"> & { id?: string }) => {
    const nextContact: FamilyContact = {
      id: draft.id ?? `contact-${Date.now()}`,
      name: draft.name,
      relation: draft.relation,
      phone: draft.phone,
    };

    const nextContacts = draft.id
      ? contacts.map((item) => (item.id === draft.id ? nextContact : item))
      : [nextContact, ...contacts];

    setContacts(nextContacts);
    window.localStorage.setItem(CONTACTS_STORAGE_KEY, JSON.stringify(nextContacts));
  };

  const saveReminder = (draft: ReminderDraftInput) => {
    const nextReminder: FamilyReminderPlan = {
      id: draft.id ?? `reminder-${Date.now()}`,
      title: draft.title,
      time: draft.time,
      frequency: draft.frequency,
      channel: draft.channel,
      status: "待同步",
    };

    const nextReminders = draft.id
      ? reminders.map((item) => (item.id === draft.id ? nextReminder : item))
      : [nextReminder, ...reminders];

    setReminders(nextReminders);
    window.localStorage.setItem(REMINDER_STORAGE_KEY, JSON.stringify(nextReminders));
    resetDismissedAlerts();
  };

  const deleteReminder = (id: string) => {
    const nextReminders = reminders.filter((item) => item.id !== id);
    setReminders(nextReminders);
    window.localStorage.setItem(REMINDER_STORAGE_KEY, JSON.stringify(nextReminders));
    resetDismissedAlerts();
  };

  const dismissAlert = (id: string) => {
    setDismissedAlerts((current) => {
      if (current.includes(id)) {
        return current;
      }
      const nextDismissedAlerts = [...current, id];
      window.localStorage.setItem(DISMISSED_ALERTS_STORAGE_KEY, JSON.stringify(nextDismissedAlerts));
      return nextDismissedAlerts;
    });
  };

  const todaySummary = useMemo(() => buildFamilyTodaySummary(reminders), [reminders]);
  const alerts = useMemo(
    () => buildFamilyAlerts(binding, reminders, contacts).filter((item) => !dismissedAlerts.includes(item.id)),
    [binding, contacts, dismissedAlerts, reminders],
  );

  const value = useMemo(
    () => ({
      binding,
      profile,
      contacts,
      reminders,
      todaySummary,
      alerts,
      bindFamily,
      updateBindingValidation,
      clearBinding,
      saveProfile,
      saveContact,
      saveReminder,
      deleteReminder,
      dismissAlert,
    }),
    [alerts, binding, contacts, profile, reminders, todaySummary],
  );

  return <FamilyAppContext.Provider value={value}>{children}</FamilyAppContext.Provider>;
}

export function useFamilyApp() {
  const context = useContext(FamilyAppContext);
  if (!context) {
    throw new Error("useFamilyApp must be used within FamilyAppProvider");
  }
  return context;
}

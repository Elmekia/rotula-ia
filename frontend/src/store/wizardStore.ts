import { create } from 'zustand'

export type WizardStep = 1 | 2 | 3 | 4

interface WizardState {
  step: WizardStep
  productId: string | null

  setStep: (step: WizardStep) => void
  nextStep: () => void
  prevStep: () => void
  setProductId: (id: string) => void
  reset: () => void
}

export const useWizardStore = create<WizardState>((set, get) => ({
  step: 1,
  productId: null,

  setStep: (step) => set({ step }),

  nextStep: () => {
    const { step } = get()
    if (step < 4) set({ step: (step + 1) as WizardStep })
  },

  prevStep: () => {
    const { step } = get()
    if (step > 1) set({ step: (step - 1) as WizardStep })
  },

  setProductId: (id) => set({ productId: id }),

  reset: () => set({ step: 1, productId: null }),
}))

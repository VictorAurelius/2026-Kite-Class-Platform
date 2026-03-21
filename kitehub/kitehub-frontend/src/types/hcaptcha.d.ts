declare module '@hcaptcha/react-hcaptcha' {
  import { Component } from 'react';

  interface HCaptchaProps {
    sitekey: string;
    onVerify?: (token: string) => void;
    onExpire?: () => void;
    onError?: (error: string) => void;
    size?: 'normal' | 'compact' | 'invisible';
    theme?: 'light' | 'dark';
    languageOverride?: string;
  }

  export default class HCaptcha extends Component<HCaptchaProps> {
    execute(): void;
    resetCaptcha(): void;
  }
}

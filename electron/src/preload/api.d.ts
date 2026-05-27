import type { DittoApi } from '../types';

declare global {
  interface Window {
    ditto: DittoApi;
  }
}

export {};

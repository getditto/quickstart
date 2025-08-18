import { Ditto } from '@dittolive/ditto';

// Global singleton to survive Fast Refresh
let globalDittoInstance: Ditto | null = null;

export const getDittoInstance = (): Ditto | null => {
  return globalDittoInstance;
};

export const setDittoInstance = (instance: Ditto): void => {
  // Clean up old instance if it exists
  if (globalDittoInstance && globalDittoInstance !== instance) {
    try {
      globalDittoInstance.stopSync();
    } catch (e) {
      console.warn('Error stopping old Ditto instance:', e);
    }
  }
  globalDittoInstance = instance;
};

export const clearDittoInstance = (): void => {
  if (globalDittoInstance) {
    try {
      globalDittoInstance.stopSync();
    } catch (e) {
      console.warn('Error stopping Ditto instance:', e);
    }
    globalDittoInstance = null;
  }
};
import {selectMode} from '../dittoMode';

describe('selectMode', () => {
  it('null token selects online', () => {
    expect(selectMode(null)).toBe('online');
  });

  it('undefined token selects online', () => {
    expect(selectMode(undefined)).toBe('online');
  });

  it('empty token selects online', () => {
    expect(selectMode('')).toBe('online');
  });

  it('whitespace-only token selects online', () => {
    expect(selectMode('   \t\n  ')).toBe('online');
  });

  it('non-empty token selects offline', () => {
    expect(selectMode('any-real-license-token')).toBe('offline');
  });
});

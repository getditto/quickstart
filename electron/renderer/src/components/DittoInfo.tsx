import React from 'react';

interface DittoInfoProps {
  appId: string;
  token: string;
  syncEnabled: boolean;
  onToggleSync: () => void;
  isInitialized: boolean;
}

const DittoInfo: React.FC<DittoInfoProps> = ({
  appId,
  token,
  syncEnabled,
  onToggleSync,
  isInitialized,
}) => {
  return (
    <div className="w-full max-w-4xl mx-auto p-6 bg-white rounded-lg shadow-md mb-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold text-gray-800">
          Ditto Electron Quickstart
        </h1>
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <div
              className={`w-3 h-3 rounded-full ${
                isInitialized ? 'bg-green-500' : 'bg-red-500'
              }`}
            />
            <span className="text-sm font-medium">
              {isInitialized ? 'Connected' : 'Disconnected'}
            </span>
          </div>
          <button
            onClick={onToggleSync}
            disabled={!isInitialized}
            className={`px-4 py-2 rounded-md text-sm font-medium ${
              syncEnabled
                ? 'bg-red-500 hover:bg-red-600 text-white'
                : 'bg-green-500 hover:bg-green-600 text-white'
            } ${
              !isInitialized
                ? 'opacity-50 cursor-not-allowed'
                : 'cursor-pointer'
            }`}
          >
            {syncEnabled ? 'Stop Sync' : 'Start Sync'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
        <div>
          <label className="block text-gray-600 font-medium mb-1">App ID</label>
          <div className="p-2 bg-gray-50 rounded border font-mono text-xs">
            {appId || 'Not configured'}
          </div>
        </div>
        <div>
          <label className="block text-gray-600 font-medium mb-1">Token</label>
          <div className="p-2 bg-gray-50 rounded border font-mono text-xs">
            {token ? `${token.substring(0, 8)}...` : 'Not configured'}
          </div>
        </div>
      </div>

      <div className="mt-4 p-4 bg-blue-50 rounded-lg">
        <h3 className="font-medium text-blue-800 mb-2">About This App</h3>
        <p className="text-sm text-blue-700">
          This Electron app demonstrates Ditto's real-time sync capabilities in
          a cross-platform desktop environment. Tasks created here will sync
          across all connected devices using Ditto's mesh networking technology.
        </p>
      </div>
    </div>
  );
};

export default DittoInfo;

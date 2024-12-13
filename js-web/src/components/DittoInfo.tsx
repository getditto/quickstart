import React from 'react';

type Props = {
  appId: string,
  token: string,
  syncEnabled: boolean,
  onToggleSync: () => void,
}

const DittoInfo: React.FC<Props> = ({ appId, token, syncEnabled, onToggleSync }) => {
  return (
    <div className='pt-8'>
      <h1 className='text-center text-6xl font-thin text-gray-700 mb-8'>Ditto Tasks</h1>
      <div className='text-center text-sm text-gray-500'>
        <p>App ID: {appId}</p>
        <p>Token: {token}</p>
        <div className='flex items-center justify-center gap-2 mt-4'>
          <span className={`${syncEnabled ? 'text-blue-600 font-medium' : ''}`}>
            {syncEnabled ? 'Sync Enabled' : 'Sync Disabled'}
          </span>
          <button
            onClick={onToggleSync}
            className={`relative inline-flex h-6 w-11 items-center rounded-full ${syncEnabled ? 'bg-blue-600' : 'bg-gray-200'
              }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition ${syncEnabled ? 'translate-x-6' : 'translate-x-1'
                }`}
            />
          </button>
        </div>
      </div>
    </div>
  );
};

export default DittoInfo;

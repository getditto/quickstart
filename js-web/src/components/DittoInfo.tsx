import React from 'react';

type Props = {
  appId: string,
  token: string,
}

const DittoInfo: React.FC<Props> = ({ appId, token }) => {
  return (
    <div className='pt-8'>
      <h1 className='text-center text-6xl font-thin text-gray-700 mb-8'>Ditto Tasks</h1>
      <div className='text-center text-sm text-gray-500'>
        <p>App ID: {appId}</p>
        <p>Token: {token}</p>
      </div>
    </div>
  );
};

export default DittoInfo;

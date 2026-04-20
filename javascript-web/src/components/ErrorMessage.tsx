import React, { useState } from 'react';

type Props = {
  error: Error;
};

const ErrorMessage: React.FC<Props> = ({ error }) => {
  const [dismissed, setDismissed] = useState(false);
  if (dismissed) return null;

  return (
    <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-red-100 text-red-700 p-6 rounded shadow-lg">
      <div className="flex justify-between items-center">
        <p>
          <b>Error</b>: {error.message}
        </p>
        <button
          onClick={() => setDismissed(true)}
          className="ml-4 text-red-700 hover:text-red-900"
        >
          &times;
        </button>
      </div>
    </div>
  );
};

export default ErrorMessage;

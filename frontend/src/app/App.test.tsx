import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';
import { AppProviders } from './providers/AppProviders';

describe('App', () => {
  it('renders the KODA dashboard shell', () => {
    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(screen.getByText('KODA PLATFORM')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
  });
});
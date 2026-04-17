import { NextResponse } from 'next/server';

/**
 * Health check endpoint for Docker container monitoring
 * Used by Docker HEALTHCHECK and load balancers
 */
export async function GET() {
  return NextResponse.json(
    {
      status: 'ok',
      timestamp: new Date().toISOString(),
      uptime: process.uptime(),
      environment: process.env.NODE_ENV || 'development',
    },
    { status: 200 }
  );
}

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone', // Enable standalone output for Docker builds
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'cdn.kiteclass.com',
      },
    ],
  },
};

module.exports = nextConfig;

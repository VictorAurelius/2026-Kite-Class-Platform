import { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/api/', '/dashboard/', '/admin/', '/customer/'],
    },
    sitemap: 'https://kitehub.vn/sitemap.xml',
  };
}

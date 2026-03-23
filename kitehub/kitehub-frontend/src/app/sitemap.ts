import { MetadataRoute } from 'next';

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: 'https://kitehub.vn', lastModified: new Date(), changeFrequency: 'weekly', priority: 1 },
    { url: 'https://kitehub.vn/pricing', lastModified: new Date(), changeFrequency: 'monthly', priority: 0.8 },
    { url: 'https://kitehub.vn/login', lastModified: new Date(), changeFrequency: 'yearly', priority: 0.3 },
    { url: 'https://kitehub.vn/register', lastModified: new Date(), changeFrequency: 'yearly', priority: 0.5 },
  ];
}

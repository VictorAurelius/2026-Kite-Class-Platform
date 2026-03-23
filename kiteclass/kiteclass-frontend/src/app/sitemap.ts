import { MetadataRoute } from 'next';

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: 'https://kiteclass.com', lastModified: new Date(), priority: 1 },
    { url: 'https://kiteclass.com/about', lastModified: new Date(), priority: 0.7 },
    { url: 'https://kiteclass.com/catalog', lastModified: new Date(), priority: 0.8 },
  ];
}

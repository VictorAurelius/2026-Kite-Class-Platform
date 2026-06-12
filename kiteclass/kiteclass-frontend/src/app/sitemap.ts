import { MetadataRoute } from 'next';

const BASE_URL = process.env.NEXT_PUBLIC_APP_URL || 'https://kitehub.me';

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  // Static routes
  const staticRoutes: MetadataRoute.Sitemap = [
    {
      url: BASE_URL,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 1,
    },
    {
      url: `${BASE_URL}/about`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.8,
    },
    {
      url: `${BASE_URL}/catalog`,
      lastModified: new Date(),
      changeFrequency: 'weekly',
      priority: 0.9,
    },
    {
      url: `${BASE_URL}/contact`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.7,
    },
    {
      url: `${BASE_URL}/login`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.5,
    },
    {
      url: `${BASE_URL}/register`,
      lastModified: new Date(),
      changeFrequency: 'monthly',
      priority: 0.5,
    },
  ];

  // Dynamic course routes (fetch from API if available)
  let courseRoutes: MetadataRoute.Sitemap = [];
  try {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';
    const res = await fetch(`${apiUrl}/api/v1/courses?page=0&size=100`, {
      next: { revalidate: 3600 },
    });
    if (res.ok) {
      const data = await res.json();
      const courses = data?.data?.content || [];
      courseRoutes = courses.map((course: { id: number; updatedAt?: string }) => ({
        url: `${BASE_URL}/catalog/${course.id}`,
        lastModified: course.updatedAt ? new Date(course.updatedAt) : new Date(),
        changeFrequency: 'weekly' as const,
        priority: 0.7,
      }));
    }
  } catch {
    // API unavailable at build time — skip dynamic routes
  }

  return [...staticRoutes, ...courseRoutes];
}

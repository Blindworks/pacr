export type CompetitionImageCategory = 'marathon' | 'city' | 'ultra';

export const COMPETITION_IMAGE_COUNTS: Record<CompetitionImageCategory, number> = {
  marathon: 3,
  city: 3,
  ultra: 3,
};

const TYPE_TO_CATEGORY: Record<string, CompetitionImageCategory> = {
  'Marathon': 'marathon',
  'Halbmarathon': 'marathon',
  '10K': 'city',
  '5K': 'city',
  '50K': 'ultra',
  '100K': 'ultra',
  'Backyard Ultra': 'ultra',
  'Catcher car': 'ultra',
};

export function categoryForType(type?: string): CompetitionImageCategory {
  return TYPE_TO_CATEGORY[type ?? ''] ?? 'city';
}

export function imagePath(category: CompetitionImageCategory, index: number): string {
  return `assets/images/competitions/${category}-${index}.webp`;
}

export function defaultImageIndex(category: CompetitionImageCategory, id: number = 0): number {
  return (Math.abs(id) % COMPETITION_IMAGE_COUNTS[category]) + 1;
}

export function resolveImage(type: string | undefined, id: number, imageIndex?: number | null): string {
  const category = categoryForType(type);
  const count = COMPETITION_IMAGE_COUNTS[category];
  let index: number;
  if (imageIndex != null && imageIndex >= 1 && imageIndex <= count) {
    index = imageIndex;
  } else {
    index = defaultImageIndex(category, id);
  }
  return imagePath(category, index);
}

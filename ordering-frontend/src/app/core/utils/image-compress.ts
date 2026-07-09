export interface CompressOptions {
  maxPx?: number;
  quality?: number;
  format?: 'image/jpeg' | 'image/webp';
}

/**
 * Compresses an image File using canvas.
 * Returns a base64 data URL ready to send to the backend.
 */
export function compressImage(file: File, opts: CompressOptions = {}): Promise<string> {
  const { maxPx = 1200, quality = 0.8, format = 'image/jpeg' } = opts;

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Impossibile leggere il file'));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => reject(new Error('Impossibile decodificare l\'immagine'));
      img.onload = () => {
        let w = img.naturalWidth;
        let h = img.naturalHeight;

        if (w > maxPx || h > maxPx) {
          const scale = Math.min(maxPx / w, maxPx / h);
          w = Math.round(w * scale);
          h = Math.round(h * scale);
        }

        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d')!;
        ctx.drawImage(img, 0, 0, w, h);
        resolve(canvas.toDataURL(format, quality));
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  });
}

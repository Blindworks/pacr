import * as L from 'leaflet';

declare module 'leaflet' {
  interface HotlineOptions extends L.PolylineOptions {
    min?: number;
    max?: number;
    palette?: Record<number, string>;
    weight?: number;
    outlineWidth?: number;
    outlineColor?: string;
  }

  class Hotline extends L.Polyline {
    constructor(latlngs: [number, number, number][], options?: HotlineOptions);
    setData(latlngs: [number, number, number][]): this;
  }

  function hotline(latlngs: [number, number, number][], options?: HotlineOptions): Hotline;
}

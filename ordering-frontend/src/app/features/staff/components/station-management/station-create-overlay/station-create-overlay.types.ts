export type StationCreatePayload = {
  name: string;
  type: 'TABLE' | 'UMBRELLA' | 'SUNBED' | 'LOUNGE' | 'VIP_AREA' | 'SERVICE_POINT';
  areaId: number;
  capacity: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'ORDERING_DISABLED' | 'CLOSED';
  notes: string;
  active: boolean;
  generateQrAutomatically: boolean;
};

export type StationTypeOption = {
  value: StationCreatePayload['type'];
  label: string;
};

export type StationStatusOption = {
  value: StationCreatePayload['status'];
  label: string;
};

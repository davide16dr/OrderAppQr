export type OrderStatus = 'received' | 'delivered';

export interface OrderLine {
  quantity: number;
  name: string;
  total: number;
  variant?: string;
  extras?: string[];
}

export interface StaffOrderCard {
  id: number;
  code: string;
  locationLabel: string;
  areaName: string;
  type: 'BAR' | 'KITCHEN';
  minutesAgo: number;
  status: OrderStatus;
  items: OrderLine[];
  total: number;
  note?: string;
  timeLabel?: string;
  dateLabel?: string;
}
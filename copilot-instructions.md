# Project context and coding instructions

You are assisting on a full-stack SaaS web application for venue ordering.

---

## Product overview

This platform is a multi-tenant ordering system for physical venues such as:
- beach clubs
- bars
- restaurants
- nightclubs

Each tenant represents a business and has:
- its own subdomain
- its own staff dashboard
- its own menu (tenant products)
- its own locations (tables, umbrellas, etc.)
- its own QR codes

Customers scan a QR code associated with a specific location and:
- open the tenant menu
- select products
- send orders to staff

Staff members manage incoming orders through a dashboard.

---

## User types

There are only two application user types exposed in the product:
- Customer
- Staff

There is NO tenant-facing admin panel.

---

## Platform administration (IMPORTANT)

Platform administration is handled internally by us.

We manage:
- tenant activation
- subscription status
- approval/rejection of registrations
- staff access bootstrap
- global catalog

This means:

There is a separate internal "platform admin" layer (not visible to tenants).

---

## Tenant lifecycle (CRITICAL)

A tenant can be created in two ways:
- SELF_SIGNUP (business registers autonomously)
- INTERNAL_CREATE (created by platform)

Tenant statuses:
- PENDING → registered but NOT operational
- ACTIVE → fully operational
- SUSPENDED → temporarily blocked
- DISABLED → permanently blocked

Rules:
- tenants start as PENDING
- tenants cannot operate unless ACTIVE
- activation can happen:
  - manually (platform admin)
  - later via subscription payment (future feature)

---

## Business registration flow

The system supports autonomous business signup.

Flow:
1. business submits registration
2. a record is created in business_registration_requests
3. a tenant is created with status = PENDING
4. a primary staff user is created
5. a tenant_subscription is created with status = PENDING
6. platform admin must approve or activate tenant

Important:
- signup does NOT activate the tenant
- signup does NOT grant full access

---

## Subscription model

Each tenant has a subscription.

Tables:
- tenant_subscriptions
- subscription_plans (must exist)

Rules:
- tenant_subscriptions defines current state
- subscription_plans defines plan configuration

Subscription statuses:
- PENDING
- TRIAL
- ACTIVE
- PAST_DUE
- CANCELLED
- EXPIRED

Important:
- tenant ACTIVE status is separate from subscription status
- both must be valid for full operation

---

## Tech stack

Frontend:
- Angular
- standalone components
- Angular Material
- SCSS
- mobile-first for customer
- desktop-oriented for staff

Backend:
- Spring Boot
- PostgreSQL
- Flyway
- REST APIs
- SSE for realtime updates

---

## Architecture goals

Write clean, modular, production-ready code.

Use:
- controller → service → repository
- DTOs for API contracts
- validation at boundaries
- domain logic in services

Avoid:
- overengineering
- unnecessary abstractions

---

## Multi-tenant rules (CRITICAL)

Each request must be scoped to a tenant.

Tenant is resolved via:
- subdomain
- or explicit context when needed

Rules:
- always enforce tenant_id consistency
- never mix tenant data
- always validate tenant ownership

---

## Domain model

Core entities:

- Tenant
- Area
- Location
- LocationToken
- Category
- GlobalCategory
- GlobalProduct
- TenantProduct
- ModifierGroup
- ModifierOption
- StaffUser
- StaffRole
- BusinessRegistrationRequest
- TenantSubscription
- SubscriptionPlan
- Order
- OrderItem
- OrderStatusHistory
- TenantStatusHistory

---

## Location and QR logic

Location = physical service point

LocationToken = QR token pointing to a location

Rules:
- QR identifies token, not directly location
- one primary active token per location
- tokens can be rotated
- old tokens must be revoked
- locations should NOT be deleted (use DISABLED)

---

## Product catalog logic

Two-layer model:

### Global catalog
- global_products
- global_categories
- managed by platform

### Tenant catalog
- tenant_products
- actual sellable products

Rules:
- global products are templates
- tenant products are real products
- menu must read from tenant_products

---

## Image logic

- global_products → default_image_url
- tenant_products → image_url (real one used)

Rule:
always use tenant_products.image_url in UI

---

## Ordering flow

Customer:
1. scan QR
2. resolve location_token
3. load tenant menu
4. create order

Staff:
1. login
2. view orders
3. update order status

---

## Order status rules (MVP)

Allowed:
- NEW
- DELIVERED
- optionally CANCELLED

Valid transitions:
- NEW → DELIVERED
- NEW → CANCELLED

Do NOT implement full workflow yet.

---

## Historical consistency rules

Orders must store snapshots:

- product_name_snapshot
- unit_price_snapshot
- department_snapshot
- location_label_snapshot
- area_name_snapshot
- modifier names and prices

Never rely on live data for past orders.

---

## Platform admin requirements (IMPORTANT)

There must be backend support for:

Tenant management:
- list tenants
- view tenant details
- activate tenant
- suspend tenant
- disable tenant

Registration management:
- list registration requests
- approve request
- reject request

Subscription management:
- view tenant subscription
- change plan (future)

These APIs must exist even if frontend admin UI is not yet implemented.

---

## Frontend rules

Structure Angular app by:
- customer
- staff
- shared
- core
- platform (internal admin, future)

Customer routes:
- /l/:token
- cart
- order confirmation

Staff routes:
- /staff/login
- /staff/dashboard

---

## Backend rules

Use:
- entities
- repositories
- services
- controllers
- DTOs

Rules:
- validation at DTO level
- business logic in service layer
- no business logic in controllers

Use Flyway for DB changes.

Use SSE for realtime.

---

## Naming conventions

Use consistent naming:

- tenant
- location
- locationToken
- tenantProduct
- globalProduct
- order
- orderItem

Avoid synonyms.

---

## Development priorities

1. QR/location resolution
2. menu loading
3. order creation
4. staff login
5. staff dashboard
6. realtime updates
7. tenant registration
8. platform admin tools

---

## Expected output style

When generating code:

- keep it clean and production-ready
- avoid unnecessary abstraction
- keep files focused
- respect architecture
- follow naming conventions

If a feature touches:
- tenant isolation
- QR logic
- catalog logic
- order snapshots
- tenant lifecycle

then strictly enforce domain rules above.
You are working on a full-stack SaaS multi-tenant ordering platform built with Spring Boot, PostgreSQL, Flyway, and Angular.

I need you to implement the business self-signup flow for companies that want to register autonomously on the platform.

## Product context
The platform is used by venues such as:
- beach clubs
- bars
- restaurants
- nightclubs

Each company is a tenant.
Each tenant has:
- its own subdomain
- its own staff dashboard
- its own locations and QR codes
- its own tenant products

The platform is multi-tenant and tenant isolation is mandatory.

## Important business rules
There are only two application user types exposed in the product:
- Customer
- Staff

Platform administration is handled internally by us.

A company must be able to register autonomously by filling in its business data and the data of the main contact person.

After registration:
- the tenant must be created in status `PENDING`
- the company must NOT be able to use the platform operationally yet
- the account can be activated later either:
  - manually by platform administrators
  - or in the future after subscription payment

So the signup flow creates the tenant, but the tenant remains non-operational until activation.

## Database model already defined
Use and respect this domain model:

### tenants
Important fields include:
- id
- slug
- name
- legal_name
- business_type
- status (`PENDING`, `ACTIVE`, `SUSPENDED`, `DISABLED`)
- timezone
- currency_code
- subdomain
- vat_number
- business_email
- business_phone
- address_line_1
- address_line_2
- city
- province
- postal_code
- country
- registration_source (`SELF_SIGNUP`, `INTERNAL_CREATE`)
- activation_date
- approved_at
- approved_by_staff_user_id
- branding_json
- opening_config_json
- created_at
- updated_at

### staff_users
Important fields include:
- id
- tenant_id
- first_name
- last_name
- email
- password_hash
- phone
- is_primary_contact
- invited_at
- activated_at
- last_login_at
- status
- created_at
- updated_at

### business_registration_requests
Important fields include:
- id
- requested_slug
- tenant_name
- legal_name
- business_type
- vat_number
- business_email
- business_phone
- address_line_1
- address_line_2
- city
- province
- postal_code
- country
- contact_first_name
- contact_last_name
- contact_email
- contact_phone
- password_hash
- requested_plan_code
- status (`SUBMITTED`, `APPROVED`, `REJECTED`, `CONVERTED`)
- submitted_at
- reviewed_at
- reviewed_by_staff_user_id
- rejection_reason
- tenant_id
- created_at
- updated_at

### tenant_subscriptions
Important fields include:
- id
- tenant_id
- plan_code
- status (`PENDING`, `TRIAL`, `ACTIVE`, `PAST_DUE`, `CANCELLED`, `EXPIRED`)
- billing_cycle (`MONTHLY`, `YEARLY`)
- payment_provider
- provider_customer_id
- provider_subscription_id
- payment_status (`PENDING`, `PAID`, `FAILED`, `REFUNDED`, `NONE`)
- current_period_start
- current_period_end
- trial_ends_at
- activated_at
- cancelled_at
- created_at
- updated_at

## What I want implemented
Implement the backend registration flow in Spring Boot with clean architecture:
- controller
- service
- repository
- DTOs
- proper validation
- transaction management
- clear exception handling

## Required behavior
Create a public signup flow for businesses.

### Endpoint
Implement:
`POST /public/business-signup`

### Request payload must include:
Business data:
- tenantName
- legalName
- businessType
- vatNumber
- businessEmail
- businessPhone
- addressLine1
- addressLine2
- city
- province
- postalCode
- country
- requestedSlug
- requestedPlanCode

Primary contact data:
- contactFirstName
- contactLastName
- contactEmail
- contactPhone
- password
- confirmPassword

Optional:
- billingCycle

## Registration flow behavior
When the request is valid, the backend must:

1. validate that:
   - requestedSlug is unique among active/pending tenants and open registration requests
   - businessEmail is valid
   - contactEmail is valid
   - password and confirmPassword match
   - businessType is valid

2. create a `business_registration_request` row with status `SUBMITTED`

3. create a `tenant` row with:
   - slug = requestedSlug
   - subdomain = requestedSlug
   - name = tenantName
   - legal_name = legalName
   - business_type = businessType
   - status = `PENDING`
   - registration_source = `SELF_SIGNUP`
   - all business contact/address fields copied from request

4. create the primary `staff_user` row linked to the tenant with:
   - first_name
   - last_name
   - email = contactEmail
   - phone = contactPhone
   - password_hash = BCrypt hash of password
   - is_primary_contact = true
   - status = `ACTIVE`

5. assign the `MANAGER` role to that user

6. create a `tenant_subscription` row with:
   - tenant_id
   - plan_code = requestedPlanCode
   - status = `PENDING`
   - payment_status = `PENDING`
   - billing_cycle = request value or default `MONTHLY`

7. update the `business_registration_request` row by linking the created tenant and setting status to `CONVERTED`

The whole flow must run in a single transaction.

## Response
Return a response DTO containing:
- tenantId
- tenantSlug
- tenantStatus
- message

Example message:
"Registration completed successfully. Your account is pending activation."

## Constraints and behavior after signup
Do not activate the tenant automatically.
Do not create a JWT login session in this endpoint.
Do not allow the tenant to operate yet.
The tenant must remain in `PENDING` until approved manually or activated after future payment integration.

## Additional implementation requirements
Please generate:
- request DTO with bean validation annotations
- response DTO
- controller
- service
- repositories if needed
- exception classes if needed
- role lookup logic for `MANAGER`
- password hashing using BCryptPasswordEncoder
- clean and readable code

## Naming conventions
Use these exact names when possible:
- BusinessSignupRequest
- BusinessSignupResponse
- BusinessSignupService
- BusinessSignupController

## Package conventions
Follow a typical Spring Boot package structure:
- controller
- service
- repository
- entity
- dto
- exception
- config

## Important notes
- Keep the implementation production-oriented
- Do not overengineer
- Respect the existing database model
- Use transactional service methods
- Prefer explicit code over magic abstractions
- If necessary, also generate helper methods for slug uniqueness validation and manager role assignment

Generate the code step by step, starting from DTOs, then service, then controller.
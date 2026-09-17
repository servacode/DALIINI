export type PermissionCode = string;

export type AdminIdentity = Readonly<{
  userId: string;
  displayName: string;
  permissions: readonly PermissionCode[];
}>;

export function can(identity: AdminIdentity | null, permission: PermissionCode): boolean {
  return identity?.permissions.includes(permission) ?? false;
}

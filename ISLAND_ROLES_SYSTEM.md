# Island Roles System - Implementation Complete

## Overview
Implemented a comprehensive 5-tier island role system with invitation mechanics, role management, and permission controls.

## Island Roles Hierarchy

### 1. **OWNER** (Permission: 100)
- Full control over the island
- Can delete the island
- Can transfer ownership to members
- Can promote/demote all other roles
- Can invite, kick, and manage all members

### 2. **CO-OWNER** (Permission: 80)
- Nearly full control
- **Cannot** delete the island
- **Cannot** transfer ownership
- Can promote/demote: Admin, Mod, Member
- Can invite and kick members
- Can manage island upgrades

### 3. **ADMIN** (Permission: 60)
- Can manage members below their rank
- Can promote/demote: Mod, Member
- Can invite players
- Can kick Mods and Members
- Can build and break blocks

### 4. **MOD (Moderator)** (Permission: 40)
- Can invite players to the island
- Can build and break blocks
- **Cannot** manage other members

### 5. **MEMBER** (Permission: 20)
- Basic island access
- Can build and break blocks
- **Cannot** invite or manage others

### 6. **VISITOR** (Permission: 1)
- Limited permissions (retained for future visitor system)

## Commands

### Island Management
- `/island create` - Create island (checks for existing membership)
- `/island delete` - Delete island (owner only)
- `/island home` - Teleport to your island
- `/island visit <player>` - Visit another player's island

### Member Management
- `/island invite <player>` - Invite a player (MOD+ required)
- `/island accept` - Accept pending invitation
- `/island deny` - Decline pending invitation
- `/island kick <player>` - Remove a member (ADMIN+ required, respects hierarchy)

### Role Management
- `/island promote <player> <role>` - Promote member to new role
  - Valid roles: `CO_OWNER`, `ADMIN`, `MOD`, `MEMBER`
  - Requires permission to manage target role
- `/island demote <player>` - Demote member one level
  - CO_OWNER → ADMIN → MOD → MEMBER
  - Requires permission to manage target role
- `/island transfer <player>` - Transfer ownership (owner only)
  - New owner gets OWNER role
  - Previous owner becomes CO_OWNER

## Features Implemented

### Invitation System
1. **5-Minute Expiration**: Invitations expire after 5 minutes
2. **Database Persistence**: Invites stored in `island_invites` table
3. **Automatic Cleanup**: Expired invites automatically deleted
4. **Single Island Policy**: Players with island membership cannot be invited

### Permission System
1. **Role Hierarchy**: Numerical permission levels for easy comparison
2. **canManageRole()**: Built-in method to check if role can manage another
3. **Permission Checks**: All commands verify permissions before execution

### Database Schema

#### `island_invites` Table
```sql
CREATE TABLE island_invites (
    island_id TEXT NOT NULL,
    invited_player TEXT NOT NULL,
    invited_by TEXT NOT NULL,
    invited_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    PRIMARY KEY (island_id, invited_player)
)
```

#### `island_members` Table Updates
- `role` column now supports 6 role values
- Role field is mutable to allow promotions/demotions

### Core Restrictions
1. **One Island Per Player**: Players can only own OR be member of one island
2. **Member Creation Block**: Members cannot create new islands
3. **Owner-Only Deletion**: Only island owner can delete the island
4. **Hierarchy Enforcement**: Cannot promote/demote/kick players at or above your level

## Implementation Details

### Files Modified
1. **IslandMember.java**
   - Updated `IslandRole` enum with 5 tiers + VISITOR
   - Added `canManageRole()` method
   - Made `role` field mutable with `setRole()` method

2. **IslandInvite.java** (NEW)
   - Tracks pending invitations
   - 5-minute expiration logic
   - Stores inviter UUID for notifications

3. **IslandDataManager.java**
   - Added `island_invites` table creation
   - Invitation CRUD operations: save, load, delete
   - `hasIslandMembership()` - checks if player owns or is member
   - `getPlayerIslandId()` - retrieves island ID for any membership type
   - `cleanupExpiredInvites()` - removes expired invitations

4. **IslandManager.java**
   - `invitePlayer()` - sends invitation with membership check
   - `getPlayerInvites()` - retrieves pending invites
   - `acceptInvite()` - adds player as MEMBER, removes invite
   - `declineInvite()` - removes invitation
   - `getMemberRole()` - gets player's role (OWNER or from members table)
   - `setMemberRole()` - promotes/demotes members
   - `transferOwnership()` - changes island owner, demotes old owner to CO_OWNER
   - `hasIslandMembership()` - wrapper for database check
   - `getPlayerIslandId()` - wrapper for database retrieval

5. **PlayerIsland.java**
   - Made `ownerUuid` mutable to support ownership transfer
   - Added `setOwnerUuid()` method

6. **IslandCommand.java**
   - Updated `handleIslandCreate()` - checks for membership
   - Updated `handleIslandDelete()` - verifies OWNER role
   - Implemented `handleIslandInvite()` - sends invites with MOD+ check
   - Implemented `handleIslandAccept()` - accepts first pending invite
   - Implemented `handleIslandDeny()` - declines invitation
   - Implemented `handleIslandPromote()` - promotes with permission check
   - Implemented `handleIslandDemote()` - demotes one level
   - Implemented `handleIslandTransfer()` - transfers ownership
   - Updated `handleIslandKick()` - removes members with hierarchy check
   - Updated help message and tab completion

## Usage Examples

### Inviting a Player
```
Owner: /island invite Steve
> ✓ Invitation sent to Steve!

Steve receives:
═══════════════════════════════
📨 ISLAND INVITATION
═══════════════════════════════
Owner has invited you to join their island!
Use /island accept to accept or /island deny to decline.
This invitation expires in 5 minutes.
═══════════════════════════════
```

### Accepting Invitation
```
Steve: /island accept
> ✓ You joined the island! Use /island to visit.

(Steve is now a MEMBER)
```

### Promoting Members
```
Owner: /island promote Steve ADMIN
> ✓ Steve promoted to Admin!

Steve: You have been promoted to Admin!
```

### Transferring Ownership
```
Owner: /island transfer Steve
> ✓ Island ownership transferred to Steve! You are now a Co-Owner.

Steve: You are now the owner of the island!
```

### Demoting Members
```
Owner: /island demote Steve
> ✓ Steve demoted to Moderator!

Steve: ⚠ You have been demoted to Moderator.
```

## Permission Matrix

| Action | OWNER | CO-OWNER | ADMIN | MOD | MEMBER |
|--------|-------|----------|-------|-----|--------|
| Delete Island | ✓ | ✗ | ✗ | ✗ | ✗ |
| Transfer Ownership | ✓ | ✗ | ✗ | ✗ | ✗ |
| Promote to CO_OWNER | ✓ | ✗ | ✗ | ✗ | ✗ |
| Promote to ADMIN | ✓ | ✓ | ✗ | ✗ | ✗ |
| Promote to MOD | ✓ | ✓ | ✓ | ✗ | ✗ |
| Promote to MEMBER | ✓ | ✓ | ✓ | ✗ | ✗ |
| Kick Members | ✓ | ✓ | ✓* | ✗ | ✗ |
| Invite Players | ✓ | ✓ | ✓ | ✓ | ✗ |
| Build/Break | ✓ | ✓ | ✓ | ✓ | ✓ |

*ADMIN can only kick players below their rank (MOD, MEMBER)

## Future Enhancements
1. GUI for viewing members and their roles
2. Role-based build permissions (restrict certain blocks)
3. Permission nodes for specific actions (chest access, button press, etc.)
4. Activity tracking for automatic role adjustments
5. Multiple island invitations queue (currently only shows first)
6. Timed role promotions (trial period for new members)

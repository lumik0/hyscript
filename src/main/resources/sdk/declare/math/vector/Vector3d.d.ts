
export default class Vector3d {
  constructor()
  constructor(v: Vector3d)
  constructor(x: number, y: number, z: number)
  constructor(yaw: number, pitch: number)

  assign(v: this): this
  assign(v: number): this
  assign(yaw: number, pitch: number): this
  assign(x: number, y: number, z: number): this

  add(v: this): this
  add(x: number, y: number, z: number): this
  add(v: number): this
  addScaled(v: this, s: number): this

  subtract(v: this): this
  subtract(x: number, y: number, z: number): this
  subtract(v: number): this

  negate(): this
  scale(s: number): this
  scale(p: this): this
  cross(v: this): this
  cross(v: this, res: this): this

  dot(other: this): number
  distanceTo(v: this): number
  distanceTo(x: number, y: number, z: number): number
  distanceSquaredTo(v: this): number
  distanceSquaredTo(x: number, y: number, z: number): number
  normalize(): this

  length(): number
  squaredLength(): number
  setLength(newLen: number): this
  clampLength(maxLength: number): this

  rotateX(angle: number): this
  rotateY(angle: number): this
  rotateZ(angle: number): this

  floor(): this
  ceil(): this
  clipToZero(epsilon: number): this
  closeToZero(epsilon: number): boolean
  isInside(x: number, y: number, z: number): boolean
  isFinite(): boolean
  dropHash(): this
  clone(): this

  static max(a: Vector3d, b: Vector3d): Vector3d
  static min(a: Vector3d, b: Vector3d): Vector3d
  static lerp(a: Vector3d, b: Vector3d, t: number): Vector3d
  static lerpUnclamped(a: Vector3d, b: Vector3d, t: number): Vector3d
  static directionTo(from: Vector3d, to: Vector3d): Vector3d
  static add(one: Vector3d, two: Vector3d): Vector3d
  static add(one: Vector3d, two: Vector3d, three: Vector3d): Vector3d
  static formatShortString(v: Vector3d): string
  // @ts-ignore TODO: vector3i
  static toVector3i(): Vector3i
  // @ts-ignore TODO: vector3f
  static toVector3f(): Vector3f

  x: number
  y: number
  z: number
}

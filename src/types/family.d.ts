export type family = {
  username: string,
  relation: string,
  gender:string,
  age: number
}

export type responseFamily = family & {
  userid: number,
  id: number
}

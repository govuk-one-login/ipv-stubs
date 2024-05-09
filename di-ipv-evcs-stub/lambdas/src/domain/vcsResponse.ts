import VcStateMetadata from "./vcStateMetadata";

export default interface VcsResponse {
  vcs: VcStateMetadata[];
  afterKey: string;
}

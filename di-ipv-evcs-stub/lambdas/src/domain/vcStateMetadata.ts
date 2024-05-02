import AllState from "./enums/allState";
import VCMetadata from "./vcMetadata";

export default interface VcStateMetadata {
    vc: string;
    state: AllState;
    metadata?: VCMetadata;
}
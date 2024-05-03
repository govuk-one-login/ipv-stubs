import VcState from "./enums/vcState";
import VCMetadata from "./vcMetadata";

export default interface VcStateMetadata {
    vc: string;
    state: VcState;
    metadata?: VCMetadata;
}
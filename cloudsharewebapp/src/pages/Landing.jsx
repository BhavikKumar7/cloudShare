import HeroSection from "../components/landing/HeroSection.jsx";
import {Fence} from "lucide-react";
import FeaturesSection from "../components/landing/FeaturesSection.jsx";
import PricingSection from "../components/landing/PricingSection.jsx";
import TestimonialsSection from "../components/landing/TestimonialsSection.jsx";
import CTASection from "../components/landing/CTASection.jsx";
import Footer from "../components/landing/Footer.jsx";
import {features, pricingPlans, testimonials} from "../assets/data.js";
import {useClerk, useUser} from "@clerk/clerk-react";
import {useNavigate} from "react-router-dom";
import {useEffect} from "react";

const Landing = () => {
    const {openSignIn, openSignUp } = useClerk();
    const {isSignedIn } = useUser();
    const navigate = useNavigate();

    useEffect(() => {
        if (isSignedIn) {
            navigate("/dashboard");
        }
    }, [isSignedIn, navigate]);

    return (
        <div className="landing-page bg-gradient-to-b from-gray-50 to-gray-100">
            <HeroSection openSignIn={openSignIn} openSignUp={openSignUp}/>

            <FeaturesSection features={features}/>

            <PricingSection pricingPlans={pricingPlans} openSignUp={openSignUp}/>

            <TestimonialsSection testimonials={testimonials} />

            <CTASection openSignUp={openSignUp} />

            <Footer />
        </div>
    )
}

export default Landing;